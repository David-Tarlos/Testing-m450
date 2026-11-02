package ch.tbz.yahoot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.tbz.yahoot.game.AnswerResult;
import ch.tbz.yahoot.game.Card;
import ch.tbz.yahoot.game.GamePhase;
import ch.tbz.yahoot.game.GameRuleException;
import ch.tbz.yahoot.game.Question;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class GameServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    private static final int RIGHT = 1;

    private final QuizRepository quizzes = mock(QuizRepository.class);
    private final GameEventPublisher events = mock(GameEventPublisher.class);
    private GameService service;

    @BeforeEach
    void setUp() {
        Duration limit = Duration.ofSeconds(20);
        Quiz quiz = new Quiz("mathe", "Kopfrechnen", List.of(
                new Question("2 + 2?", List.of("3", "4"), RIGHT, limit),
                new Question("3 + 3?", List.of("5", "6"), RIGHT, limit)));
        when(quizzes.findById("mathe")).thenReturn(Optional.of(quiz));
        when(quizzes.findById("gibts-nicht")).thenReturn(Optional.empty());
        service = new GameService(quizzes, events, Clock.fixed(NOW, ZoneOffset.UTC), new Random(42));
    }

    @Test
    void createdGamesGetDifferentReadableCodes() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            codes.add(service.createGame("mathe"));
        }

        assertThat(codes).hasSize(200);
        assertThat(codes).allMatch(code -> code.matches("[A-HJ-NP-Z2-9]{6}"));
    }

    @Test
    void unknownQuizCannotBeUsed() {
        assertThatThrownBy(() -> service.createGame("gibts-nicht")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void unknownCodeIsReported() {
        assertThatThrownBy(() -> service.join("XXXXXX", "Lena")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void codeIsCaseInsensitive() {
        String code = service.createGame("mathe");

        service.join(code.toLowerCase(), "Lena");

        assertThat(service.ranking(code)).extracting(PlayerScore::name).containsExactly("Lena");
    }

    @Test
    void joiningAnnouncesAllPlayers() {
        String code = service.createGame("mathe");

        service.join(code, "Lena");
        service.join(code, "Tim");

        verify(events).publish(new GameEvent.PlayerJoined(code, "Tim", List.of("Lena", "Tim")));
    }

    @Test
    void ruleViolationsAreNotPublished() {
        String code = service.createGame("mathe");
        service.join(code, "Lena");

        assertThatThrownBy(() -> service.join(code, "lena")).isInstanceOf(GameRuleException.class);
        verify(events, never()).publish(new GameEvent.PlayerJoined(code, "lena", List.of("Lena", "lena")));
    }

    @Test
    void startShowsTheFirstQuestionWithoutTheSolution() {
        String code = service.createGame("mathe");
        service.join(code, "Lena");

        service.start(code);

        verify(events).publish(new GameEvent.QuestionStarted(
                code, 1, 2, "2 + 2?", List.of("3", "4"), 20, List.of()));
        assertThat(service.phase(code)).isEqualTo(GamePhase.QUESTION);
    }

    @Test
    void answersUseTheTimeOfTheClock() {
        String code = service.createGame("mathe");
        service.join(code, "Lena");
        service.start(code);

        assertThat(service.answer(code, "Lena", RIGHT)).isEqualTo(AnswerResult.ACCEPTED);
        service.closeQuestion(code);

        // Die Uhr steht still, also maximale Geschwindigkeit
        assertThat(service.ranking(code)).containsExactly(new PlayerScore("Lena", 1000, 2));
    }

    @Test
    void wholeRoundPublishesEventsInOrder() {
        String code = service.createGame("mathe");
        service.join(code, "Lena");
        service.join(code, "Tim");
        service.start(code);
        service.answer(code, "Lena", RIGHT);
        service.closeQuestion(code);
        service.openSabotage(code);
        service.playCard(code, "Lena", Card.FREEZE, "Tim");
        service.nextQuestion(code);
        service.closeQuestion(code);

        InOrder order = inOrder(events);
        order.verify(events).publish(any(GameEvent.QuestionStarted.class));
        order.verify(events).publish(any(GameEvent.QuestionClosed.class));
        order.verify(events).publish(any(GameEvent.SabotageOpened.class));
        ArgumentCaptor<GameEvent.QuestionStarted> second = ArgumentCaptor.forClass(GameEvent.QuestionStarted.class);
        order.verify(events).publish(second.capture());
        order.verify(events).publish(any(GameEvent.QuestionClosed.class));
        order.verify(events).publish(any(GameEvent.GameFinished.class));

        assertThat(second.getValue().number()).isEqualTo(2);
        assertThat(second.getValue().attacks()).singleElement()
                .satisfies(attack -> assertThat(attack.target()).isEqualTo("Tim"));
    }

    @Test
    void closingRevealsTheSolutionAndTheRanking() {
        String code = service.createGame("mathe");
        service.join(code, "Lena");
        service.join(code, "Tim");
        service.start(code);
        service.answer(code, "Tim", RIGHT);

        service.closeQuestion(code);

        verify(events).publish(new GameEvent.QuestionClosed(code, 1, RIGHT, List.of(
                new PlayerScore("Tim", 1000, 2),
                new PlayerScore("Lena", 0, 2))));
    }

    @Test
    void timerOnlyClosesTheRoundItWasStartedFor() {
        String code = service.createGame("mathe");
        service.join(code, "Lena");
        service.start(code);
        service.closeQuestion(code);
        service.openSabotage(code);

        // Der Host hat schon geschlossen, der Timer der ersten Frage kommt zu spaet
        assertThat(service.closeQuestionIfOpen(code, 1)).isFalse();
        assertThat(service.phase(code)).isEqualTo(GamePhase.SABOTAGE);

        service.nextQuestion(code);
        assertThat(service.closeQuestionIfOpen(code, 1)).isFalse();
        assertThat(service.closeQuestionIfOpen(code, 2)).isTrue();
        assertThat(service.phase(code)).isEqualTo(GamePhase.FINISHED);
    }

    @Test
    void timerOnlyEndsTheSabotageOfItsOwnRound() {
        String code = service.createGame("mathe");
        service.join(code, "Lena");
        service.start(code);

        assertThat(service.nextQuestionIfSabotage(code, 1)).isFalse();
        service.closeQuestion(code);
        service.openSabotage(code);

        assertThat(service.nextQuestionIfSabotage(code, 2)).isFalse();
        assertThat(service.nextQuestionIfSabotage(code, 1)).isTrue();
        assertThat(service.phase(code)).isEqualTo(GamePhase.QUESTION);
        assertThat(service.round(code)).isEqualTo(2);
    }

    @Test
    void openingSabotageAnnouncesRoundAndTime() {
        String code = service.createGame("mathe");
        service.join(code, "Lena");
        service.start(code);
        service.closeQuestion(code);

        service.openSabotage(code);

        verify(events).publish(new GameEvent.SabotageOpened(code, 1, 10, service.ranking(code)));
    }

    @Test
    void snapshotOfTheLobbyContainsNoQuestion() {
        String code = service.createGame("mathe");
        service.join(code, "Lena");

        GameSnapshot snapshot = service.snapshot(code);

        assertThat(snapshot.phase()).isEqualTo(GamePhase.LOBBY);
        assertThat(snapshot.players()).containsExactly("Lena");
        assertThat(snapshot.text()).isNull();
        assertThat(snapshot.options()).isEmpty();
        assertThat(snapshot.correctOption()).isNull();
    }

    @Test
    void snapshotHidesTheSolutionWhileTheQuestionIsOpen() {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(NOW);
        GameService withClock = new GameService(quizzes, events, clock, new Random(42));
        String code = withClock.createGame("mathe");
        withClock.join(code, "Lena");
        withClock.start(code);
        when(clock.instant()).thenReturn(NOW.plusSeconds(5));

        GameSnapshot snapshot = withClock.snapshot(code);

        assertThat(snapshot.text()).isEqualTo("2 + 2?");
        assertThat(snapshot.round()).isEqualTo(1);
        assertThat(snapshot.total()).isEqualTo(2);
        assertThat(snapshot.secondsLeft()).isEqualTo(15);
        assertThat(snapshot.correctOption()).isNull();
    }

    @Test
    void snapshotShowsSolutionAndAttacksAfterTheQuestion() {
        String code = service.createGame("mathe");
        service.join(code, "Lena");
        service.join(code, "Tim");
        service.start(code);
        service.closeQuestion(code);
        service.openSabotage(code);
        service.playCard(code, "Lena", Card.FREEZE, "Tim");
        service.nextQuestion(code);
        service.closeQuestion(code);

        GameSnapshot snapshot = service.snapshot(code);

        assertThat(snapshot.correctOption()).isEqualTo(RIGHT);
        assertThat(snapshot.attacks()).singleElement()
                .satisfies(attack -> assertThat(attack.attacker()).isEqualTo("Lena"));
        assertThat(snapshot.secondsLeft()).isZero();
    }

    @Test
    void finishedGamesCanBeRemoved() {
        String code = service.createGame("mathe");

        service.remove(code);

        assertThatThrownBy(() -> service.phase(code)).isInstanceOf(NotFoundException.class);
    }
}

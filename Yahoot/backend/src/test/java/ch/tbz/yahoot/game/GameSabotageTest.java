package ch.tbz.yahoot.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameSabotageTest {

    private static final Instant START = Instant.parse("2026-11-01T12:00:00Z");
    private static final Instant SECOND_QUESTION = START.plusSeconds(60);
    private static final int RIGHT = 1;

    private Game game;

    /** Lena und Tim stehen nach einer richtigen Antwort mit je 2 Energie in der Sabotage-Phase. */
    @BeforeEach
    void setUp() {
        Duration limit = Duration.ofSeconds(20);
        game = new Game(List.of(
                new Question("2 + 2?", List.of("3", "4"), RIGHT, limit),
                new Question("3 + 3?", List.of("5", "6"), RIGHT, limit),
                new Question("4 + 4?", List.of("7", "8"), RIGHT, limit)));
        game.join("Lena");
        game.join("Tim");
        game.start(START);
        game.answer("Lena", RIGHT, START);
        game.answer("Tim", RIGHT, START);
        game.closeQuestion();
        game.openSabotage();
    }

    @Test
    void playingACardCostsEnergy() {
        game.playCard("Lena", Card.STEAL, "Tim");

        assertThat(game.player("Lena").energy()).isZero();
    }

    @Test
    void onlyOneCardPerPhase() {
        game.playCard("Lena", Card.FREEZE, "Tim");

        assertThatThrownBy(() -> game.playCard("Lena", Card.SHUFFLE, "Tim"))
                .isInstanceOf(GameRuleException.class);
        assertThat(game.player("Lena").energy()).isEqualTo(1);
    }

    @Test
    void cardNeedsEnoughEnergy() {
        game.playCard("Lena", Card.STEAL, "Tim");
        game.nextQuestion(SECOND_QUESTION);
        game.closeQuestion();
        game.openSabotage();

        assertThatThrownBy(() -> game.playCard("Lena", Card.FREEZE, "Tim"))
                .isInstanceOf(GameRuleException.class);
    }

    @Test
    void nobodyCanAttackThemselvesOrPlayOutsideTheSabotagePhase() {
        assertThatThrownBy(() -> game.playCard("Lena", Card.FREEZE, "Lena"))
                .isInstanceOf(GameRuleException.class);

        game.nextQuestion(SECOND_QUESTION);

        assertThatThrownBy(() -> game.playCard("Lena", Card.FREEZE, "Tim"))
                .isInstanceOf(GameRuleException.class);
    }

    @Test
    void frozenPlayerCannotAnswerDuringTheFirstThreeSeconds() {
        game.playCard("Lena", Card.FREEZE, "Tim");
        game.nextQuestion(SECOND_QUESTION);

        assertThat(game.answer("Tim", RIGHT, SECOND_QUESTION.plusMillis(2999)))
                .isEqualTo(AnswerResult.FROZEN);
        assertThat(game.answer("Tim", RIGHT, SECOND_QUESTION.plusSeconds(3)))
                .isEqualTo(AnswerResult.ACCEPTED);
    }

    @Test
    void freezeOnlyLastsForOneQuestion() {
        game.playCard("Lena", Card.FREEZE, "Tim");
        game.nextQuestion(SECOND_QUESTION);
        game.closeQuestion();
        game.openSabotage();

        Instant thirdQuestion = SECOND_QUESTION.plusSeconds(60);
        game.nextQuestion(thirdQuestion);

        assertThat(game.answer("Tim", RIGHT, thirdQuestion)).isEqualTo(AnswerResult.ACCEPTED);
    }

    @Test
    void shuffleMarksOnlyTheVictim() {
        game.playCard("Lena", Card.SHUFFLE, "Tim");
        game.nextQuestion(SECOND_QUESTION);

        assertThat(game.isShuffled("Tim")).isTrue();
        assertThat(game.isShuffled("Lena")).isFalse();
    }

    private Game threePlayersInSabotage() {
        Game result = new Game(List.of(
                new Question("2 + 2?", List.of("3", "4"), RIGHT, Duration.ofSeconds(20)),
                new Question("3 + 3?", List.of("5", "6"), RIGHT, Duration.ofSeconds(20))
                ));
        result.join("Lena");
        result.join("Tim");
        result.join("Mia");
        result.start(START);
        result.answer("Lena", RIGHT, START);
        result.answer("Tim", RIGHT, START);
        result.answer("Mia", 0, START);
        result.closeQuestion();
        result.openSabotage();
        return result;
    }
}

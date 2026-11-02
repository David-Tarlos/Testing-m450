package ch.tbz.yahoot.game;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameRoundTest {

    private static final Instant START = Instant.parse("2026-11-01T12:00:00Z");
    private static final Duration LIMIT = Duration.ofSeconds(20);
    private static final int RIGHT = 1;
    private static final int WRONG = 0;

    private Game game;

    @BeforeEach
    void setUp() {
        game = new Game(List.of(
                new Question("2 + 2?", List.of("3", "4"), RIGHT, LIMIT),
                new Question("3 + 3?", List.of("5", "6"), RIGHT, LIMIT)));
        game.join("Lena");
        game.join("Tim");
        game.start(START);
    }

    @Test
    void correctAnswerGivesPointsAndEnergyAndWrongAnswerNothing() {
        game.answer("Lena", RIGHT, START);
        game.answer("Tim", WRONG, START);

        game.closeQuestion();

        assertThat(game.player("Lena").score()).isEqualTo(1000);
        assertThat(game.player("Lena").energy()).isEqualTo(2);
        assertThat(game.player("Tim").score()).isZero();
        assertThat(game.player("Tim").energy()).isEqualTo(1);
    }

    @Test
    void onlyTheFirstAnswerCounts() {
        game.answer("Lena", WRONG, START);

        assertThat(game.answer("Lena", RIGHT, START)).isEqualTo(AnswerResult.ALREADY_ANSWERED);

        game.closeQuestion();
        assertThat(game.player("Lena").score()).isZero();
    }

    @Test
    void answerOutsideTheOptionsIsRejectedAndDoesNotCount() {
        assertThat(game.answer("Lena", 2, START)).isEqualTo(AnswerResult.INVALID_OPTION);
        assertThat(game.answer("Lena", -1, START)).isEqualTo(AnswerResult.INVALID_OPTION);

        assertThat(game.answer("Lena", RIGHT, START)).isEqualTo(AnswerResult.ACCEPTED);
    }

    @Test
    void answerAfterTheTimeLimitIsRejected() {
        assertThat(game.answer("Lena", RIGHT, START.plus(LIMIT).plusMillis(1)))
                .isEqualTo(AnswerResult.TOO_LATE);
    }

    @Test
    void roundsRunThroughAllPhasesUntilTheGameIsFinished() {
        game.closeQuestion();
        assertThat(game.phase()).isEqualTo(GamePhase.REVEAL);

        game.openSabotage();
        assertThat(game.phase()).isEqualTo(GamePhase.SABOTAGE);

        game.nextQuestion(START.plusSeconds(60));
        assertThat(game.phase()).isEqualTo(GamePhase.QUESTION);
        assertThat(game.currentQuestion().text()).isEqualTo("3 + 3?");

        game.closeQuestion();
        assertThat(game.phase()).isEqualTo(GamePhase.FINISHED);
    }
}

package ch.tbz.yahoot.game;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ScoreCalculatorTest {

    private static final Duration LIMIT = Duration.ofSeconds(20);

    @Test
    void wrongAnswerGivesNoPoints() {
        assertThat(ScoreCalculator.points(false, Duration.ZERO, LIMIT)).isZero();
    }

    @Test
    void instantCorrectAnswerGivesMaximum() {
        assertThat(ScoreCalculator.points(true, Duration.ZERO, LIMIT)).isEqualTo(1000);
    }

    @Test
    void correctAnswerAtLastMomentGivesBasePoints() {
        assertThat(ScoreCalculator.points(true, LIMIT, LIMIT)).isEqualTo(500);
    }

    @Test
    void answerAfterTheLimitGivesNoPoints() {
        assertThat(ScoreCalculator.points(true, LIMIT.plusMillis(1), LIMIT)).isZero();
    }
}

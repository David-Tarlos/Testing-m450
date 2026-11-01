package ch.tbz.yahoot.game;

import java.time.Duration;

/** Richtige Antworten geben 500 Punkte plus bis zu 500 Bonus, je schneller desto mehr. */
final class ScoreCalculator {

    private static final int BASE_POINTS = 500;
    private static final int MAX_SPEED_BONUS = 500;

    private ScoreCalculator() {
    }

    static int points(boolean correct, Duration answerTime, Duration limit) {
        if (!correct || answerTime.compareTo(limit) > 0) {
            return 0;
        }
        double remainingShare = 1 - (double) answerTime.toMillis() / limit.toMillis();
        return BASE_POINTS + (int) Math.round(MAX_SPEED_BONUS * remainingShare);
    }
}

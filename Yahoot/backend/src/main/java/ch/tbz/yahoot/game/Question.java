package ch.tbz.yahoot.game;

import java.time.Duration;
import java.util.List;

public record Question(String text, List<String> options, int correctOption, Duration timeLimit) {

    public Question {
        options = List.copyOf(options);
        if (options.size() < 2) {
            throw new IllegalArgumentException("Eine Frage braucht mindestens zwei Optionen");
        }
        if (timeLimit == null || timeLimit.isZero() || timeLimit.isNegative()) {
            throw new IllegalArgumentException("Das Zeitlimit muss positiv sein");
        }
        if (correctOption < 0 || correctOption >= options.size()) {
            throw new IllegalArgumentException("Die richtige Antwort muss zu den Optionen gehoeren");
        }
    }
}

package ch.tbz.yahoot.game;

import java.time.Duration;
import java.util.List;

public record Question(String text, List<String> options, int correctOption, Duration timeLimit) {

    public Question {
        options = List.copyOf(options);
        if (correctOption < 0 || correctOption >= options.size()) {
            throw new IllegalArgumentException("Die richtige Antwort muss zu den Optionen gehoeren");
        }
    }
}

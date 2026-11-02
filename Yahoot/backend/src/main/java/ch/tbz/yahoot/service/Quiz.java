package ch.tbz.yahoot.service;

import ch.tbz.yahoot.game.Question;
import java.util.List;

public record Quiz(String id, String title, List<Question> questions) {

    public Quiz {
        questions = List.copyOf(questions);
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("Ein Quiz braucht mindestens eine Frage");
        }
    }
}

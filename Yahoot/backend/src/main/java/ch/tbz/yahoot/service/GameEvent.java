package ch.tbz.yahoot.service;

import ch.tbz.yahoot.game.Attack;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

/**
 * Alles, was der Server an Beamer und Handys meldet. Die richtige Antwort steht erst im Reveal. Im JSON
 * steht der Name des Ereignisses im Feld {@code type}, zum Beispiel {@code "type":"PlayerJoined"}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "type")
public sealed interface GameEvent {

    String code();

    record PlayerJoined(String code, String name, List<String> players) implements GameEvent {
    }

    record QuestionStarted(String code, int number, int total, String text, List<String> options,
            long seconds, List<Attack> attacks) implements GameEvent {
    }

    record QuestionClosed(String code, int number, int correctOption, List<PlayerScore> ranking)
            implements GameEvent {
    }

    record SabotageOpened(String code, List<PlayerScore> ranking) implements GameEvent {
    }

    record GameFinished(String code, List<PlayerScore> ranking) implements GameEvent {
    }
}

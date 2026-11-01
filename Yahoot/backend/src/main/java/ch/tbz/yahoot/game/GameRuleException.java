package ch.tbz.yahoot.game;

/** Eine Aktion verstoesst gegen die Spielregeln. */
public class GameRuleException extends RuntimeException {

    public GameRuleException(String message) {
        super(message);
    }
}

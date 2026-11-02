package ch.tbz.yahoot.service;

/** Ein Spiel oder Quiz mit diesem Code gibt es nicht. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}

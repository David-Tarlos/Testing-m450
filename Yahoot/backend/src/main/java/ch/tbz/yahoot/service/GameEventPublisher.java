package ch.tbz.yahoot.service;

/** Verschickt Ereignisse an die Clients. Im Test wird diese Schnittstelle gemockt. */
public interface GameEventPublisher {

    void publish(GameEvent event);
}

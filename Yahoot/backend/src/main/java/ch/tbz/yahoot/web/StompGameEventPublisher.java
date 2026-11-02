package ch.tbz.yahoot.web;

import ch.tbz.yahoot.service.GameEvent;
import ch.tbz.yahoot.service.GameEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Schickt jedes Ereignis an alle im Spiel und meldet es zusaetzlich intern, etwa an den Timer. */
@Component
class StompGameEventPublisher implements GameEventPublisher {

    private final SimpMessagingTemplate messaging;
    private final ApplicationEventPublisher internal;

    StompGameEventPublisher(SimpMessagingTemplate messaging, ApplicationEventPublisher internal) {
        this.messaging = messaging;
        this.internal = internal;
    }

    @Override
    public void publish(GameEvent event) {
        messaging.convertAndSend("/topic/games/" + event.code(), event);
        internal.publishEvent(event);
    }
}

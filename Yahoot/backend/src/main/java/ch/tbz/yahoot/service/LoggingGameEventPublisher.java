package ch.tbz.yahoot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Platzhalter, bis der Versand ueber WebSocket steht: schreibt die Ereignisse nur ins Log. */
@Component
class LoggingGameEventPublisher implements GameEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingGameEventPublisher.class);

    @Override
    public void publish(GameEvent event) {
        LOG.info("{}", event);
    }
}

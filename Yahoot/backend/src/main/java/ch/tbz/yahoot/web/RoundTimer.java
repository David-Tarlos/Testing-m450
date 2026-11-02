package ch.tbz.yahoot.web;

import ch.tbz.yahoot.service.GameEvent;
import ch.tbz.yahoot.service.GameService;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

/**
 * Schliesst eine Frage und beendet die Sabotage-Phase, sobald die Zeit um ist, auch wenn der Host nichts
 * tut. Fertige Spiele werden nach einer halben Stunde aus dem Speicher entfernt.
 */
@Component
class RoundTimer {

    /** Kleiner Puffer, damit eine Antwort in der letzten Millisekunde noch ankommt. */
    private static final Duration GRACE = Duration.ofMillis(300);
    private static final Duration KEEP_FINISHED = Duration.ofMinutes(30);

    private final GameService games;
    private final TaskScheduler scheduler;

    RoundTimer(GameService games, @Qualifier("roundScheduler") TaskScheduler scheduler) {
        this.games = games;
        this.scheduler = scheduler;
    }

    @EventListener
    void onQuestionStarted(GameEvent.QuestionStarted event) {
        Instant end = Instant.now().plusSeconds(event.seconds()).plus(GRACE);
        scheduler.schedule(() -> games.closeQuestionIfOpen(event.code(), event.number()), end);
    }

    @EventListener
    void onSabotageOpened(GameEvent.SabotageOpened event) {
        Instant end = Instant.now().plusSeconds(event.seconds());
        scheduler.schedule(() -> games.nextQuestionIfSabotage(event.code(), event.number()), end);
    }

    @EventListener
    void onGameFinished(GameEvent.GameFinished event) {
        scheduler.schedule(() -> games.remove(event.code()), Instant.now().plus(KEEP_FINISHED));
    }
}

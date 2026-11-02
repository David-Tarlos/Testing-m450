package ch.tbz.yahoot.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ch.tbz.yahoot.service.GameEvent;
import ch.tbz.yahoot.service.GameService;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

class RoundTimerTest {

    private final GameService games = mock(GameService.class);

    @Test
    void closesTheRoundItWasStartedForAfterTheTimeLimit() {
        RoundTimer timer = new RoundTimer(games, new SimpleAsyncTaskScheduler());

        timer.onQuestionStarted(new GameEvent.QuestionStarted("ABCDEF", 2, 3, "?", List.of("a", "b"), 0, List.of()));

        verify(games, org.mockito.Mockito.timeout(TimeUnit.SECONDS.toMillis(2))).closeQuestionIfOpen("ABCDEF", 2);
    }

    @Test
    void endsTheSabotagePhaseOfItsRoundAfterTheTime() {
        RoundTimer timer = new RoundTimer(games, new SimpleAsyncTaskScheduler());

        timer.onSabotageOpened(new GameEvent.SabotageOpened("ABCDEF", 2, 0, List.of()));

        verify(games, org.mockito.Mockito.timeout(TimeUnit.SECONDS.toMillis(2))).nextQuestionIfSabotage("ABCDEF", 2);
    }

    @Test
    void doesNothingBeforeTheTimeIsUp() throws InterruptedException {
        RoundTimer timer = new RoundTimer(games, new SimpleAsyncTaskScheduler());

        timer.onQuestionStarted(new GameEvent.QuestionStarted("ABCDEF", 1, 3, "?", List.of("a", "b"), 60, List.of()));

        Thread.sleep(200);
        verifyNoInteractions(games);
    }
}

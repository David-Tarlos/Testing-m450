package ch.tbz.yahoot.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameLobbyTest {

    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");

    private Game game;

    @BeforeEach
    void setUp() {
        game = new Game(List.of(
                new Question("2 + 2?", List.of("3", "4"), 1, Duration.ofSeconds(20))));
    }

    @Test
    void questionNeedsCorrectIndexWithinItsOptions() {
        assertThatThrownBy(() -> new Question("?", List.of("a", "b"), 2, Duration.ofSeconds(20)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nameCanOnlyBeUsedOnceIgnoringCase() {
        game.join("Lena");

        assertThatThrownBy(() -> game.join("lena")).isInstanceOf(GameRuleException.class);
        assertThat(game.ranking()).extracting(Player::name).containsExactly("Lena");
    }

    @Test
    void gameCannotStartWithoutPlayers() {
        assertThatThrownBy(() -> game.start(NOW)).isInstanceOf(GameRuleException.class);
        assertThat(game.phase()).isEqualTo(GamePhase.LOBBY);
    }

    @Test
    void answerBeforeTheStartIsRejected() {
        game.join("Lena");

        assertThat(game.answer("Lena", 1, NOW)).isEqualTo(AnswerResult.TOO_LATE);
    }

    @Test
    void nobodyCanJoinAfterTheStart() {
        game.join("Lena");
        game.start(NOW);

        assertThat(game.phase()).isEqualTo(GamePhase.QUESTION);
        assertThatThrownBy(() -> game.join("Tim")).isInstanceOf(GameRuleException.class);
    }
}

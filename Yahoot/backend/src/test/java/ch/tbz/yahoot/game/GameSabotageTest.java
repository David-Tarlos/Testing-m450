package ch.tbz.yahoot.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameSabotageTest {

    private static final Instant START = Instant.parse("2026-11-01T12:00:00Z");
    private static final Instant SECOND_QUESTION = START.plusSeconds(60);
    private static final int RIGHT = 1;

    private Game game;

    /** Lena und Tim stehen nach einer richtigen Antwort mit je 2 Energie in der Sabotage-Phase. */
    @BeforeEach
    void setUp() {
        Duration limit = Duration.ofSeconds(20);
        game = new Game(List.of(
                new Question("2 + 2?", List.of("3", "4"), RIGHT, limit),
                new Question("3 + 3?", List.of("5", "6"), RIGHT, limit),
                new Question("4 + 4?", List.of("7", "8"), RIGHT, limit)));
        game.join("Lena");
        game.join("Tim");
        game.start(START);
        game.answer("Lena", RIGHT, START);
        game.answer("Tim", RIGHT, START);
        game.closeQuestion();
        game.openSabotage();
    }

    @Test
    void playingACardCostsEnergy() {
        game.playCard("Lena", Card.STEAL, "Tim");

        assertThat(game.player("Lena").energy()).isZero();
    }

    @Test
    void onlyOneCardPerPhase() {
        game.playCard("Lena", Card.FREEZE, "Tim");

        assertThatThrownBy(() -> game.playCard("Lena", Card.SHUFFLE, "Tim"))
                .isInstanceOf(GameRuleException.class);
        assertThat(game.player("Lena").energy()).isEqualTo(1);
    }

    @Test
    void cardNeedsEnoughEnergy() {
        game.playCard("Lena", Card.STEAL, "Tim");
        game.nextQuestion(SECOND_QUESTION);
        game.closeQuestion();
        game.openSabotage();

        assertThatThrownBy(() -> game.playCard("Lena", Card.FREEZE, "Tim"))
                .isInstanceOf(GameRuleException.class);
    }

    @Test
    void nobodyCanAttackThemselvesOrPlayOutsideTheSabotagePhase() {
        assertThatThrownBy(() -> game.playCard("Lena", Card.FREEZE, "Lena"))
                .isInstanceOf(GameRuleException.class);

        game.nextQuestion(SECOND_QUESTION);

        assertThatThrownBy(() -> game.playCard("Lena", Card.FREEZE, "Tim"))
                .isInstanceOf(GameRuleException.class);
    }

    @Test
    void frozenPlayerCannotAnswerDuringTheFirstThreeSeconds() {
        game.playCard("Lena", Card.FREEZE, "Tim");
        game.nextQuestion(SECOND_QUESTION);

        assertThat(game.answer("Tim", RIGHT, SECOND_QUESTION.plusMillis(2999)))
                .isEqualTo(AnswerResult.FROZEN);
        assertThat(game.answer("Tim", RIGHT, SECOND_QUESTION.plusSeconds(3)))
                .isEqualTo(AnswerResult.ACCEPTED);
    }

    @Test
    void freezeOnlyLastsForOneQuestion() {
        game.playCard("Lena", Card.FREEZE, "Tim");
        game.nextQuestion(SECOND_QUESTION);
        game.closeQuestion();
        game.openSabotage();

        Instant thirdQuestion = SECOND_QUESTION.plusSeconds(60);
        game.nextQuestion(thirdQuestion);

        assertThat(game.answer("Tim", RIGHT, thirdQuestion)).isEqualTo(AnswerResult.ACCEPTED);
    }

    @Test
    void shuffleMarksOnlyTheVictim() {
        game.playCard("Lena", Card.SHUFFLE, "Tim");
        game.nextQuestion(SECOND_QUESTION);

        assertThat(game.isShuffled("Tim")).isTrue();
        assertThat(game.isShuffled("Lena")).isFalse();
    }

    @Test
    void stealTakesHalfOfTheVictimsPointsForThatQuestion() {
        game.playCard("Lena", Card.STEAL, "Tim");
        game.nextQuestion(SECOND_QUESTION);
        game.answer("Tim", RIGHT, SECOND_QUESTION);

        game.closeQuestion();

        assertThat(game.player("Tim").score()).isEqualTo(1000 + 500);
        assertThat(game.player("Lena").score()).isEqualTo(1000 + 500);
    }

    @Test
    void stealGivesNothingWhenTheVictimAnswersWrong() {
        game.playCard("Lena", Card.STEAL, "Tim");
        game.nextQuestion(SECOND_QUESTION);
        game.answer("Tim", 0, SECOND_QUESTION);

        game.closeQuestion();

        assertThat(game.player("Lena").score()).isEqualTo(1000);
        assertThat(game.player("Tim").score()).isEqualTo(1000);
    }

    @Test
    void secondThiefOnlyGetsHalfOfWhatIsLeft() {
        Game threePlayers = threePlayersInSabotage();
        threePlayers.playCard("Lena", Card.STEAL, "Tim");
        threePlayers.playCard("Mia", Card.STEAL, "Tim");
        threePlayers.nextQuestion(SECOND_QUESTION);
        threePlayers.answer("Tim", RIGHT, SECOND_QUESTION);

        threePlayers.closeQuestion();

        assertThat(threePlayers.player("Lena").score()).isEqualTo(1000 + 500);
        assertThat(threePlayers.player("Mia").score()).isEqualTo(250);
        assertThat(threePlayers.player("Tim").score()).isEqualTo(1000 + 250);
    }

    @Test
    void shieldBlocksTheAttackAndTheRevealShowsIt() {
        game.playCard("Lena", Card.FREEZE, "Tim");
        game.playCard("Tim", Card.SHIELD, "Tim");

        game.nextQuestion(SECOND_QUESTION);

        assertThat(game.answer("Tim", RIGHT, SECOND_QUESTION)).isEqualTo(AnswerResult.ACCEPTED);
        assertThat(game.lastAttacks())
                .containsExactly(new Attack("Lena", Card.FREEZE, "Tim", true));
    }

    @Test
    void shieldBlocksOnlyOneAttack() {
        Game threePlayers = threePlayersInSabotage();
        threePlayers.playCard("Lena", Card.FREEZE, "Tim");
        threePlayers.playCard("Mia", Card.SHUFFLE, "Tim");
        threePlayers.playCard("Tim", Card.SHIELD, "Tim");

        threePlayers.nextQuestion(SECOND_QUESTION);

        assertThat(threePlayers.lastAttacks()).containsExactly(
                new Attack("Lena", Card.FREEZE, "Tim", true),
                new Attack("Mia", Card.SHUFFLE, "Tim", false));
        assertThat(threePlayers.isShuffled("Tim")).isTrue();
    }

    @Test
    void lastThirdOfTheRankingGetsExtraEnergy() {
        Game threePlayers = threePlayersInSabotage();

        // Mia antwortet falsch und liegt hinten: Start 1 + Comeback 1
        assertThat(threePlayers.player("Mia").energy()).isEqualTo(2);
        assertThat(threePlayers.player("Lena").energy()).isEqualTo(2);
        assertThat(threePlayers.player("Tim").energy()).isEqualTo(2);
    }

    @Test
    void withTwoPlayersTheLastOneStillGetsComebackEnergy() {
        Game twoPlayers = gameWith("Lena", "Tim");
        twoPlayers.answer("Lena", RIGHT, START);
        twoPlayers.closeQuestion();

        assertThat(twoPlayers.player("Lena").energy()).isEqualTo(2);
        assertThat(twoPlayers.player("Tim").energy()).isEqualTo(2);
    }

    @Test
    void playersTiedWithTheLastOneAlsoGetComebackEnergy() {
        Game fourPlayers = gameWith("Lena", "Tim", "Mia", "Noah");
        fourPlayers.answer("Lena", RIGHT, START);
        fourPlayers.answer("Tim", RIGHT, START);
        fourPlayers.closeQuestion();

        // Ein Drittel von 4 ist 1 Platz, Mia und Noah teilen sich aber den letzten Rang.
        assertThat(fourPlayers.player("Mia").energy()).isEqualTo(2);
        assertThat(fourPlayers.player("Noah").energy()).isEqualTo(2);
    }

    @Test
    void nobodyGetsComebackEnergyWhenAllAreTied() {
        Game threePlayers = gameWith("Lena", "Tim", "Mia");
        threePlayers.closeQuestion();

        assertThat(threePlayers.ranking()).extracting(Player::energy).containsOnly(1);
    }

    private Game gameWith(String... names) {
        Game result = new Game(List.of(
                new Question("2 + 2?", List.of("3", "4"), RIGHT, Duration.ofSeconds(20)),
                new Question("3 + 3?", List.of("5", "6"), RIGHT, Duration.ofSeconds(20))));
        for (String name : names) {
            result.join(name);
        }
        result.start(START);
        return result;
    }

    private Game threePlayersInSabotage() {
        Game result = new Game(List.of(
                new Question("2 + 2?", List.of("3", "4"), RIGHT, Duration.ofSeconds(20)),
                new Question("3 + 3?", List.of("5", "6"), RIGHT, Duration.ofSeconds(20))
                ));
        result.join("Lena");
        result.join("Tim");
        result.join("Mia");
        result.start(START);
        result.answer("Lena", RIGHT, START);
        result.answer("Tim", RIGHT, START);
        result.answer("Mia", 0, START);
        result.closeQuestion();
        result.openSabotage();
        return result;
    }
}

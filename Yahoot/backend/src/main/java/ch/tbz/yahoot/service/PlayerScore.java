package ch.tbz.yahoot.service;

import ch.tbz.yahoot.game.Player;

/** Ein Eintrag der Rangliste, so wie ihn Beamer und Handy sehen. */
public record PlayerScore(String name, int score, int energy) {

    static PlayerScore of(Player player) {
        return new PlayerScore(player.name(), player.score(), player.energy());
    }
}

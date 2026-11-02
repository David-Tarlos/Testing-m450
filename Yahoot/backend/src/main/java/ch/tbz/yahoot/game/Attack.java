package ch.tbz.yahoot.game;

/** Ein gespielter Angriff, wie ihn die Auflösung auf dem Beamer zeigt. */
public record Attack(String attacker, Card card, String target, boolean blocked) {
}

package ch.tbz.yahoot.service;

import ch.tbz.yahoot.game.Attack;
import ch.tbz.yahoot.game.GamePhase;
import java.util.List;

/**
 * Der ganze Spielstand auf einmal, für alle, die neu laden oder die Verbindung verloren haben. Die richtige
 * Antwort ist {@code null}, solange die Frage offen ist.
 */
public record GameSnapshot(String code, GamePhase phase, int round, int total, String text, List<String> options,
        long secondsLeft, Integer correctOption, List<Attack> attacks, List<String> players,
        List<PlayerScore> ranking) {
}

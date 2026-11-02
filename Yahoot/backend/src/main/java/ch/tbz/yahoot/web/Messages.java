package ch.tbz.yahoot.web;

import ch.tbz.yahoot.game.AnswerResult;
import ch.tbz.yahoot.game.Card;
import ch.tbz.yahoot.game.GamePhase;
import ch.tbz.yahoot.service.PlayerScore;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Die Nachrichten zwischen Clients und Server, wie sie als JSON verschickt werden. */
final class Messages {

    private Messages() {
    }

    record CreateGameRequest(@NotBlank String quizId) {
    }

    record CreatedGame(String code, String hostToken) {
    }

    record QuizSummary(String id, String title, int questions) {
    }

    record GameState(String code, GamePhase phase, int round, List<PlayerScore> ranking) {
    }

    record JoinRequest(String name) {
    }

    record Joined(String code, String name) {
    }

    record AnswerRequest(int option) {
    }

    record AnswerReply(AnswerResult result) {
    }

    record CardRequest(Card card, String target) {
    }

    record HostCommand(String hostToken) {
    }

    record ErrorMessage(String message) {
    }
}

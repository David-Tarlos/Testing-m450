package ch.tbz.yahoot.web;

import ch.tbz.yahoot.game.AnswerResult;
import ch.tbz.yahoot.game.Card;
import jakarta.validation.constraints.NotBlank;

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

    record JoinRequest(String name) {
    }

    record RejoinRequest(String playerToken) {
    }

    record Joined(String code, String name, String playerToken) {
    }

    record CardPlayed(Card card, String target) {
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

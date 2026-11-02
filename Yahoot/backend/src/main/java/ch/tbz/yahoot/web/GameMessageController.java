package ch.tbz.yahoot.web;

import ch.tbz.yahoot.game.GameRuleException;
import ch.tbz.yahoot.service.GameService;
import ch.tbz.yahoot.service.NotFoundException;
import ch.tbz.yahoot.web.Messages.AnswerReply;
import ch.tbz.yahoot.web.Messages.AnswerRequest;
import ch.tbz.yahoot.web.Messages.CardRequest;
import ch.tbz.yahoot.web.Messages.ErrorMessage;
import ch.tbz.yahoot.web.Messages.HostCommand;
import ch.tbz.yahoot.web.Messages.JoinRequest;
import ch.tbz.yahoot.web.Messages.Joined;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

/** Nimmt die Aktionen von Host und Handys entgegen. Fehler gehen nur an den, der sie ausgeloest hat. */
@Controller
@MessageMapping("/games/{code}")
class GameMessageController {

    private static final Logger LOG = LoggerFactory.getLogger(GameMessageController.class);
    private static final String SESSION = "simpSessionId";

    private final GameService games;
    private final HostTokens hosts;
    private final PlayerSessions players;

    GameMessageController(GameService games, HostTokens hosts, PlayerSessions players) {
        this.games = games;
        this.hosts = hosts;
        this.players = players;
    }

    @MessageMapping("/join")
    @SendToUser(destinations = "/queue/joined", broadcast = false)
    Joined join(@DestinationVariable String code, @Payload JoinRequest request, @Header(SESSION) String session) {
        games.join(code, request.name());
        String name = request.name().strip();
        players.register(session, code, name);
        return new Joined(code.strip().toUpperCase(java.util.Locale.ROOT), name);
    }

    @MessageMapping("/answer")
    @SendToUser(destinations = "/queue/answers", broadcast = false)
    AnswerReply answer(@DestinationVariable String code, @Payload AnswerRequest request,
            @Header(SESSION) String session) {
        return new AnswerReply(games.answer(code, players.playerIn(session, code), request.option()));
    }

    @MessageMapping("/card")
    void playCard(@DestinationVariable String code, @Payload CardRequest request, @Header(SESSION) String session) {
        if (request.card() == null) {
            throw new GameRuleException("Keine Karte gewaehlt");
        }
        games.playCard(code, players.playerIn(session, code), request.card(), request.target());
    }

    @MessageMapping("/start")
    void start(@DestinationVariable String code, @Payload HostCommand command) {
        hosts.check(code, command.hostToken());
        games.start(code);
    }

    @MessageMapping("/close")
    void close(@DestinationVariable String code, @Payload HostCommand command) {
        hosts.check(code, command.hostToken());
        games.closeQuestion(code);
    }

    @MessageMapping("/sabotage")
    void sabotage(@DestinationVariable String code, @Payload HostCommand command) {
        hosts.check(code, command.hostToken());
        games.openSabotage(code);
    }

    @MessageMapping("/next")
    void next(@DestinationVariable String code, @Payload HostCommand command) {
        hosts.check(code, command.hostToken());
        games.nextQuestion(code);
    }

    @MessageExceptionHandler({GameRuleException.class, NotFoundException.class,
            HostTokens.NotHostException.class, IllegalArgumentException.class})
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    ErrorMessage handleRuleViolation(RuntimeException e) {
        return new ErrorMessage(e.getMessage());
    }

    @MessageExceptionHandler
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    ErrorMessage handleUnexpected(Exception e) {
        LOG.warn("Unerwarteter Fehler bei einer Nachricht", e);
        return new ErrorMessage("Da ist etwas schiefgelaufen");
    }
}

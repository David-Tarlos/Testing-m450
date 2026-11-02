package ch.tbz.yahoot.web;

import ch.tbz.yahoot.game.GameRuleException;
import ch.tbz.yahoot.service.GameService;
import ch.tbz.yahoot.service.GameSnapshot;
import ch.tbz.yahoot.service.NotFoundException;
import ch.tbz.yahoot.service.QuizRepository;
import ch.tbz.yahoot.web.Messages.CreateGameRequest;
import ch.tbz.yahoot.web.Messages.CreatedGame;
import ch.tbz.yahoot.web.Messages.QuizSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Alles, was kein Live-Ereignis ist: Quiz auflisten, Spiel erstellen, Stand abfragen. */
@RestController
@RequestMapping("/api")
class GameRestController {

    private final GameService games;
    private final QuizRepository quizzes;
    private final HostTokens hosts;

    GameRestController(GameService games, QuizRepository quizzes, HostTokens hosts) {
        this.games = games;
        this.quizzes = quizzes;
        this.hosts = hosts;
    }

    @GetMapping("/quizzes")
    List<QuizSummary> quizzes() {
        return quizzes.findAll().stream()
                .map(quiz -> new QuizSummary(quiz.id(), quiz.title(), quiz.questions().size()))
                .toList();
    }

    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    CreatedGame create(@Valid @RequestBody CreateGameRequest request) {
        String code = games.createGame(request.quizId());
        return new CreatedGame(code, hosts.issue(code));
    }

    @GetMapping("/games/{code}")
    GameSnapshot state(@PathVariable String code) {
        return games.snapshot(code);
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(GameRuleException.class)
    ProblemDetail conflict(GameRuleException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}

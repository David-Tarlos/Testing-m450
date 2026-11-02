package ch.tbz.yahoot.service;

import ch.tbz.yahoot.game.AnswerResult;
import ch.tbz.yahoot.game.Card;
import ch.tbz.yahoot.game.Game;
import ch.tbz.yahoot.game.GamePhase;
import ch.tbz.yahoot.game.Player;
import ch.tbz.yahoot.game.Question;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Haelt die laufenden Spiele im Arbeitsspeicher und meldet jede Aenderung. Die Regeln selbst stecken in
 * {@link Game}. Pro Spiel wird synchronisiert, weil Host, Handys und Timer gleichzeitig zugreifen.
 */
@Service
public class GameService {

    /** Ohne 0, O, 1 und I, damit man den Code vom Beamer fehlerfrei abtippen kann. */
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final long SABOTAGE_SECONDS = 10;

    private final QuizRepository quizzes;
    private final GameEventPublisher events;
    private final Clock clock;
    private final RandomGenerator random;
    private final Map<String, RunningGame> games = new ConcurrentHashMap<>();

    @Autowired
    public GameService(QuizRepository quizzes, GameEventPublisher events, Clock clock) {
        this(quizzes, events, clock, new SecureRandom());
    }

    GameService(QuizRepository quizzes, GameEventPublisher events, Clock clock, RandomGenerator random) {
        this.quizzes = quizzes;
        this.events = events;
        this.clock = clock;
        this.random = random;
    }

    public String createGame(String quizId) {
        Quiz quiz = quizzes.findById(quizId)
                .orElseThrow(() -> new NotFoundException("Unbekanntes Quiz: " + quizId));
        RunningGame running = new RunningGame(new Game(quiz.questions()), quiz.questions().size());
        String code;
        do {
            code = newCode();
        } while (games.putIfAbsent(code, running) != null);
        return code;
    }

    public void join(String code, String name) {
        inGame(code, game -> {
            game.game.join(name);
            // In der Lobby haben alle 0 Punkte, die Rangliste steht also in der Reihenfolge des Beitritts.
            List<String> names = game.game.ranking().stream().map(Player::name).toList();
            events.publish(new GameEvent.PlayerJoined(normalize(code), game.game.player(name).name(), names));
            return null;
        });
    }

    public void start(String code) {
        inGame(code, game -> {
            game.game.start(clock.instant());
            game.round = 1;
            events.publish(questionStarted(normalize(code), game));
            return null;
        });
    }

    public AnswerResult answer(String code, String player, int option) {
        return inGame(code, game -> game.game.answer(player, option, clock.instant()));
    }

    public void closeQuestion(String code) {
        inGame(code, game -> {
            close(normalize(code), game);
            return null;
        });
    }

    /** Fuer den Timer: schliesst nur, wenn die Frage dieser Runde noch offen ist. */
    public boolean closeQuestionIfOpen(String code, int round) {
        return inGame(code, game -> {
            if (game.round != round || game.game.phase() != GamePhase.QUESTION) {
                return false;
            }
            close(normalize(code), game);
            return true;
        });
    }

    public void openSabotage(String code) {
        inGame(code, game -> {
            game.game.openSabotage();
            events.publish(new GameEvent.SabotageOpened(normalize(code), game.round, SABOTAGE_SECONDS, ranking(game)));
            return null;
        });
    }

    /** Fuer den Timer: geht nur weiter, wenn die Sabotage-Phase dieser Runde noch laeuft. */
    public boolean nextQuestionIfSabotage(String code, int round) {
        return inGame(code, game -> {
            if (game.round != round || game.game.phase() != GamePhase.SABOTAGE) {
                return false;
            }
            advance(normalize(code), game);
            return true;
        });
    }

    public void playCard(String code, String player, Card card, String target) {
        inGame(code, game -> {
            game.game.playCard(player, card, target);
            return null;
        });
    }

    public void nextQuestion(String code) {
        inGame(code, game -> {
            advance(normalize(code), game);
            return null;
        });
    }

    public GameSnapshot snapshot(String code) {
        return inGame(code, game -> {
            GamePhase phase = game.game.phase();
            Question question = phase == GamePhase.LOBBY ? null : game.game.currentQuestion();
            boolean solved = question != null && phase != GamePhase.QUESTION;
            long millisLeft = game.game.timeLeft(clock.instant()).toMillis();
            return new GameSnapshot(normalize(code), phase, game.round, game.total,
                    question == null ? null : question.text(),
                    question == null ? List.of() : question.options(),
                    (millisLeft + 999) / 1000,
                    solved ? Integer.valueOf(question.correctOption()) : null,
                    game.game.lastAttacks(),
                    game.game.ranking().stream().map(Player::name).toList(),
                    ranking(game));
        });
    }

    public GamePhase phase(String code) {
        return inGame(code, game -> game.game.phase());
    }

    public int round(String code) {
        return inGame(code, game -> game.round);
    }

    public boolean isShuffled(String code, String player) {
        return inGame(code, game -> game.game.isShuffled(player));
    }

    public List<PlayerScore> ranking(String code) {
        return inGame(code, GameService::ranking);
    }

    public void remove(String code) {
        games.remove(normalize(code));
    }

    private void advance(String code, RunningGame game) {
        game.game.nextQuestion(clock.instant());
        game.round++;
        events.publish(questionStarted(code, game));
    }

    private void close(String code, RunningGame game) {
        game.game.closeQuestion();
        Question question = game.game.currentQuestion();
        List<PlayerScore> ranking = ranking(game);
        events.publish(new GameEvent.QuestionClosed(code, game.round, question.correctOption(), ranking));
        if (game.game.phase() == GamePhase.FINISHED) {
            events.publish(new GameEvent.GameFinished(code, ranking));
        }
    }

    private static GameEvent.QuestionStarted questionStarted(String code, RunningGame game) {
        Question question = game.game.currentQuestion();
        return new GameEvent.QuestionStarted(code, game.round, game.total, question.text(),
                question.options(), question.timeLimit().toSeconds(), game.game.lastAttacks());
    }

    private static List<PlayerScore> ranking(RunningGame game) {
        return game.game.ranking().stream().map(PlayerScore::of).toList();
    }

    private <T> T inGame(String code, Function<RunningGame, T> action) {
        RunningGame game = games.get(normalize(code));
        if (game == null) {
            throw new NotFoundException("Kein Spiel mit dem Code " + code);
        }
        synchronized (game) {
            return action.apply(game);
        }
    }

    private String newCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    private static String normalize(String code) {
        return code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
    }

    private static final class RunningGame {
        private final Game game;
        private final int total;
        private int round;

        private RunningGame(Game game, int total) {
            this.game = game;
            this.total = total;
        }
    }
}

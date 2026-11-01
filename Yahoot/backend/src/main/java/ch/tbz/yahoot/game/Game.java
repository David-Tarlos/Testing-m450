package ch.tbz.yahoot.game;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Die Spielregeln einer Partie. Reines Java: Die aktuelle Zeit wird immer von aussen uebergeben. */
public final class Game {

    private final List<Question> questions;
    private final Map<String, Player> players = new LinkedHashMap<>();
    private final Map<Player, Answer> answers = new LinkedHashMap<>();
    private GamePhase phase = GamePhase.LOBBY;
    private int questionIndex;
    private Instant questionStartedAt;

    public Game(List<Question> questions) {
        this.questions = List.copyOf(questions);
    }

    public void join(String name) {
        requirePhase(GamePhase.LOBBY);
        if (players.putIfAbsent(key(name), new Player(name)) != null) {
            throw new GameRuleException("Name ist bereits vergeben: " + name);
        }
    }

    public void start(Instant now) {
        requirePhase(GamePhase.LOBBY);
        if (players.isEmpty()) {
            throw new GameRuleException("Ohne Spieler kann das Spiel nicht starten");
        }
        beginQuestion(0, now);
    }

    public AnswerResult answer(String playerName, int option, Instant now) {
        Player player = player(playerName);
        Duration answerTime = Duration.between(questionStartedAt, now);
        if (phase != GamePhase.QUESTION || answerTime.compareTo(currentQuestion().timeLimit()) > 0) {
            return AnswerResult.TOO_LATE;
        }
        if (answers.containsKey(player)) {
            return AnswerResult.ALREADY_ANSWERED;
        }
        answers.put(player, new Answer(option, answerTime));
        return AnswerResult.ACCEPTED;
    }

    public void closeQuestion() {
        requirePhase(GamePhase.QUESTION);
        Question question = currentQuestion();
        answers.forEach((player, answer) -> {
            boolean correct = answer.option() == question.correctOption();
            player.addScore(ScoreCalculator.points(correct, answer.time(), question.timeLimit()));
            if (correct) {
                player.addEnergy(1);
            }
        });
        phase = isLastQuestion() ? GamePhase.FINISHED : GamePhase.REVEAL;
    }

    public void openSabotage() {
        requirePhase(GamePhase.REVEAL);
        phase = GamePhase.SABOTAGE;
    }

    public void nextQuestion(Instant now) {
        requirePhase(GamePhase.SABOTAGE);
        beginQuestion(questionIndex + 1, now);
    }

    public GamePhase phase() {
        return phase;
    }

    public Question currentQuestion() {
        return questions.get(questionIndex);
    }

    public Player player(String name) {
        Player player = players.get(key(name));
        if (player == null) {
            throw new GameRuleException("Unbekannter Spieler: " + name);
        }
        return player;
    }

    /** Spieler nach Punkten, bei Gleichstand in der Reihenfolge des Beitritts. */
    public List<Player> ranking() {
        List<Player> ranking = new ArrayList<>(players.values());
        ranking.sort(Comparator.comparingInt(Player::score).reversed());
        return ranking;
    }

    private void beginQuestion(int index, Instant now) {
        questionIndex = index;
        questionStartedAt = now;
        answers.clear();
        phase = GamePhase.QUESTION;
    }

    private boolean isLastQuestion() {
        return questionIndex == questions.size() - 1;
    }

    private void requirePhase(GamePhase expected) {
        if (phase != expected) {
            throw new GameRuleException("Das geht nur in der Phase " + expected);
        }
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private record Answer(int option, Duration time) {
    }
}

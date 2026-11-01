package ch.tbz.yahoot.game;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Die Spielregeln einer Partie. Reines Java: Die aktuelle Zeit wird immer von aussen uebergeben. */
public final class Game {

    private static final Duration FREEZE_DURATION = Duration.ofSeconds(3);

    private final List<Question> questions;
    private final Set<Player> frozen = new HashSet<>();
    private final Set<Player> shuffled = new HashSet<>();
    private final Map<Player, List<Player>> thieves = new HashMap<>();
    private final List<Attack> lastAttacks = new ArrayList<>();
    private final Map<String, Player> players = new LinkedHashMap<>();
    private final Map<Player, Answer> answers = new LinkedHashMap<>();
    private final Map<Player, PlayedCard> playedCards = new LinkedHashMap<>();
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
        if (frozen.contains(player) && answerTime.compareTo(FREEZE_DURATION) < 0) {
            return AnswerResult.FROZEN;
        }
        answers.put(player, new Answer(option, answerTime));
        return AnswerResult.ACCEPTED;
    }

    public void closeQuestion() {
        requirePhase(GamePhase.QUESTION);
        Question question = currentQuestion();
        answers.forEach((player, answer) -> {
            boolean correct = answer.option() == question.correctOption();
            int points = ScoreCalculator.points(correct, answer.time(), question.timeLimit());
            player.addScore(points - stealFrom(player, points));
            if (correct) {
                player.addEnergy(1);
            }
        });
        giveComebackEnergy();
        phase = isLastQuestion() ? GamePhase.FINISHED : GamePhase.REVEAL;
    }

    private void giveComebackEnergy() {
        List<Player> ranking = ranking();
        int lastThird = ranking.size() / 3;
        ranking.subList(ranking.size() - lastThird, ranking.size())
                .forEach(player -> player.addEnergy(1));
    }

    public void openSabotage() {
        requirePhase(GamePhase.REVEAL);
        phase = GamePhase.SABOTAGE;
    }

    public void playCard(String playerName, Card card, String targetName) {
        requirePhase(GamePhase.SABOTAGE);
        Player player = player(playerName);
        Player target = player(targetName);
        if (playedCards.containsKey(player)) {
            throw new GameRuleException("Pro Sabotage-Phase ist nur eine Karte erlaubt");
        }
        boolean mustTargetSelf = card == Card.SHIELD;
        if (mustTargetSelf != (target == player)) {
            throw new GameRuleException("Das Schild gilt nur fuer dich, Angriffe nur fuer andere");
        }
        if (player.energy() < card.cost()) {
            throw new GameRuleException("Zu wenig Energie fuer " + card);
        }
        player.addEnergy(-card.cost());
        playedCards.put(player, new PlayedCard(card, target));
    }

    public void nextQuestion(Instant now) {
        requirePhase(GamePhase.SABOTAGE);
        beginQuestion(questionIndex + 1, now);
        applyPlayedCards();
        playedCards.clear();
    }

    public GamePhase phase() {
        return phase;
    }

    public Question currentQuestion() {
        return questions.get(questionIndex);
    }

    /** Die Angriffe aus der letzten Sabotage-Phase, in der Reihenfolge, in der sie gespielt wurden. */
    public List<Attack> lastAttacks() {
        return List.copyOf(lastAttacks);
    }

    public boolean isShuffled(String playerName) {
        return shuffled.contains(player(playerName));
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
        frozen.clear();
        shuffled.clear();
        thieves.clear();
        phase = GamePhase.QUESTION;
    }

    /** Schilde zaehlen zuerst, damit sie auch gegen Angriffe aus derselben Phase wirken. */
    private void applyPlayedCards() {
        lastAttacks.clear();
        playedCards.forEach((player, played) -> {
            if (played.card() == Card.SHIELD) {
                player.raiseShield();
            }
        });
        playedCards.forEach((player, played) -> {
            if (played.card() != Card.SHIELD) {
                applyAttack(player, played);
            }
        });
    }

    private void applyAttack(Player attacker, PlayedCard played) {
        Player target = played.target();
        boolean blocked = target.consumeShield();
        lastAttacks.add(new Attack(attacker.name(), played.card(), target.name(), blocked));
        if (blocked) {
            return;
        }
        switch (played.card()) {
            case FREEZE -> frozen.add(target);
            case SHUFFLE -> shuffled.add(target);
            case STEAL -> thieves.computeIfAbsent(target, victim -> new ArrayList<>()).add(attacker);
            case SHIELD -> throw new IllegalArgumentException("Das Schild ist kein Angriff");
        }
    }

    /** Jeder Dieb bekommt die Hälfte dessen, was das Opfer noch übrig hat. */
    private int stealFrom(Player victim, int points) {
        int remaining = points;
        for (Player thief : thieves.getOrDefault(victim, List.of())) {
            int stolen = remaining / 2;
            thief.addScore(stolen);
            remaining -= stolen;
        }
        return points - remaining;
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

    private record PlayedCard(Card card, Player target) {
    }
}

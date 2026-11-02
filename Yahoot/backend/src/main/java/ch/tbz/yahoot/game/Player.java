package ch.tbz.yahoot.game;

public final class Player {

    private static final int START_ENERGY = 1;

    private final String name;
    private int score;
    private int energy = START_ENERGY;
    private boolean shielded;

    Player(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public int score() {
        return score;
    }

    public int energy() {
        return energy;
    }

    void addScore(int points) {
        score += points;
    }

    void addEnergy(int amount) {
        energy += amount;
    }

    void raiseShield() {
        shielded = true;
    }

    /** Verbraucht ein vorhandenes Schild und meldet, ob eines da war. */
    boolean consumeShield() {
        boolean hadShield = shielded;
        shielded = false;
        return hadShield;
    }
}

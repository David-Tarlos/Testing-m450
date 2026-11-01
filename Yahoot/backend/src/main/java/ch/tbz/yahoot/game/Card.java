package ch.tbz.yahoot.game;

public enum Card {
    FREEZE(1),
    SHUFFLE(1),
    STEAL(2),
    SHIELD(1);

    private final int cost;

    Card(int cost) {
        this.cost = cost;
    }

    public int cost() {
        return cost;
    }
}

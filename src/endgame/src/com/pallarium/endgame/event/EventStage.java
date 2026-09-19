package com.pallarium.endgame.event;

/** One step of a world event. Immutable definition, progress lives on ActiveEvent. */
public class EventStage {

    private final StageType type;
    private final String label;
    private final String hint;
    private final int goal;
    private final int seconds;

    public EventStage(StageType type, String label, String hint, int goal, int seconds) {
        this.type = type;
        this.label = label;
        this.hint = hint;
        this.goal = goal;
        this.seconds = seconds;
    }

    public static EventStage kill(String label, String hint, int goal, int seconds) {
        return new EventStage(StageType.KILL, label, hint, goal, seconds);
    }

    public static EventStage boss(String label, String hint, int seconds) {
        return new EventStage(StageType.BOSS, label, hint, 1, seconds);
    }

    public static EventStage defend(String label, String hint, int seconds) {
        return new EventStage(StageType.DEFEND, label, hint, seconds, seconds);
    }

    public static EventStage brk(String label, String hint, int goal, int seconds) {
        return new EventStage(StageType.BREAK, label, hint, goal, seconds);
    }

    public static EventStage escort(String label, String hint, int seconds) {
        return new EventStage(StageType.ESCORT, label, hint, 1, seconds);
    }

    public static EventStage deliver(String label, String hint, int goal, int seconds) {
        return new EventStage(StageType.DELIVER, label, hint, goal, seconds);
    }

    /** Light N braziers by standing on them. */
    public static EventStage ritual(String label, String hint, int goal, int seconds) {
        return new EventStage(StageType.RITUAL, label, hint, goal, seconds);
    }

    /** Run down a fleeing target before it escapes the ring. */
    public static EventStage chase(String label, String hint, int seconds) {
        return new EventStage(StageType.CHASE, label, hint, 1, seconds);
    }

    /** Pick up N essences dropped by event mobs. */
    public static EventStage collect(String label, String hint, int goal, int seconds) {
        return new EventStage(StageType.COLLECT, label, hint, goal, seconds);
    }

    /** Keep the core standing for N seconds. */
    public static EventStage protect(String label, String hint, int seconds) {
        return new EventStage(StageType.PROTECT, label, hint, seconds, seconds);
    }

    public StageType type() {
        return type;
    }

    public String label() {
        return label;
    }

    public String hint() {
        return hint;
    }

    public int goal() {
        return goal;
    }

    public int seconds() {
        return seconds;
    }
}

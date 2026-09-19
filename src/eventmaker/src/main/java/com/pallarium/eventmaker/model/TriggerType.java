package com.pallarium.eventmaker.model;

public enum TriggerType {
    MANUAL("Manual", "Only starts when you press Start."),
    INTERVAL("Repeating", "Starts automatically every X minutes.");

    public final String label;
    public final String description;

    TriggerType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public TriggerType next() {
        return values()[(ordinal() + 1) % values().length];
    }
}

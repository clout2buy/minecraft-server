package com.pallarium.endgame.event;

/** What a single stage of a world event asks the players to do. */
public enum StageType {
    /** Kill N event mobs inside the radius. */
    KILL,
    /** Kill one champion boss. */
    BOSS,
    /** Stay alive and keep the area held for N seconds. */
    DEFEND,
    /** Break N marked blocks. */
    BREAK,
    /** Walk a friendly NPC to the destination. */
    ESCORT,
    /** Throw N of a requested item onto the altar. */
    DELIVER,
    /** Stand on each brazier until it catches. Light N of them. */
    RITUAL,
    /** Run down a fleeing thief before it clears the ring. */
    CHASE,
    /** Pick up N soul essences torn out of the mobs you kill. */
    COLLECT,
    /** Keep the core alive for N seconds while they tear at it. */
    PROTECT
}

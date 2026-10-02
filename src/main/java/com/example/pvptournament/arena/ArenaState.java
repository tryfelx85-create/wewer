package com.example.pvptournament.arena;

public enum ArenaState {
    /** Idle, no players, ready to be filled */
    WAITING,
    /** Enough players queued, countdown running, join still allowed up to maxPlayers */
    STARTING,
    /** Match running, join blocked (unless spectator) */
    INGAME,
    /** Win condition met, showing results, about to reset */
    ENDING,
    /** World/arena resetting (schematic repaste, chest refill, etc.) */
    RESETTING,
    /** Disabled by admin, cannot be used */
    DISABLED
}

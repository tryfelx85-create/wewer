package com.example.pvptournament.mode;

/**
 * The supported match formats. Each arena declares which type(s) it supports.
 * SKYWARS is a specialization that also drives island placement / chest refill.
 */
public enum GameModeType {
    DUEL_1V1(2, 2, false),
    TEAM(4, 16, true),
    FFA(3, 12, false),
    SKYWARS_SOLO(2, 12, false),
    SKYWARS_TEAMS(4, 16, true);

    private final int minPlayers;
    private final int maxPlayers;
    private final boolean teamBased;

    GameModeType(int minPlayers, int maxPlayers, boolean teamBased) {
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.teamBased = teamBased;
    }

    public int minPlayers() {
        return minPlayers;
    }

    public int maxPlayers() {
        return maxPlayers;
    }

    public boolean isTeamBased() {
        return teamBased;
    }

    public boolean isSkyWars() {
        return this == SKYWARS_SOLO || this == SKYWARS_TEAMS;
    }
}

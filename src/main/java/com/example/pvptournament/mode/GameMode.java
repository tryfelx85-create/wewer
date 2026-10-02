package com.example.pvptournament.mode;

import com.example.pvptournament.arena.Arena;
import org.bukkit.entity.Player;

/**
 * Pluggable match logic. Each GameModeType has a corresponding GameMode implementation
 * that the ArenaRuntime delegates to for mode-specific behavior (teleporting players in,
 * win condition checks, death handling, etc).
 *
 * Implementations: DuelMode, TeamMode, FfaMode, SkyWarsMode
 */
public interface GameMode {

    GameModeType type();

    /** Called once when the arena transitions STARTING -> INGAME. Teleport players, give kits, etc. */
    void onMatchStart(Arena arena);

    /** Called whenever a player in this arena dies. Return true if the match should now end. */
    boolean onPlayerDeath(Arena arena, Player victim, Player killer);

    /** Called when a player disconnects or is removed mid-match. Return true if match should end. */
    boolean onPlayerLeave(Arena arena, Player player);

    /** Called once per second (or tick interval) while INGAME, for things like border shrink / timers. */
    default void onTick(Arena arena) {
    }

    /** Called when the arena transitions INGAME -> ENDING. Announce winner, freeze players, etc. */
    void onMatchEnd(Arena arena);

    /** Reset any mode-specific world state (chests, islands, blocks placed). Called before WAITING. */
    void resetArenaWorld(Arena arena);
}

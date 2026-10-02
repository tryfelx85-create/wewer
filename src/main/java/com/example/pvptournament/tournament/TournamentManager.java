package com.example.pvptournament.tournament;

import com.example.pvptournament.PvPTournamentPlugin;
import com.example.pvptournament.arena.Arena;
import com.example.pvptournament.arena.ArenaManager;
import com.example.pvptournament.arena.ArenaState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Admin-driven tournament orchestration. Admins create a tournament, players register,
 * admin starts the bracket, and this manager assigns READY matches onto free arenas
 * from the tournament's arena pool, then listens for arena match completion to advance
 * the bracket automatically.
 *
 * Hook: call onArenaMatchFinished(arena, winnerUuid) from wherever match results are
 * finalized (e.g. from CombatListener or ArenaManager) when that arena was running a
 * tournament match, so the bracket can progress.
 */
public class TournamentManager {

    private final PvPTournamentPlugin plugin;
    private final ArenaManager arenaManager;

    private final Map<String, Tournament> tournaments = new LinkedHashMap<>();
    // arenaId -> the BracketMatch currently being played there, so results route back correctly
    private final Map<String, BracketMatch> arenaToMatch = new HashMap<>();

    public TournamentManager(PvPTournamentPlugin plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    public Tournament create(String id, Tournament.Format format, com.example.pvptournament.mode.GameModeType modeType, List<String> arenaPool) {
        Tournament t = new Tournament(id, format, modeType, arenaPool);
        tournaments.put(id, t);
        return t;
    }

    public Tournament get(String id) {
        return tournaments.get(id);
    }

    public Collection<Tournament> getAll() {
        return tournaments.values();
    }

    public void delete(String id) {
        tournaments.remove(id);
    }

    /** Admin command entry point: generates the bracket and begins assigning matches to arenas. */
    public void startTournament(Tournament tournament) {
        tournament.startBracket();
        broadcastToRegistered(tournament, "§6Tournament §e" + tournament.getId() + " §6bracket has been generated! Matches starting soon.");
        assignReadyMatches(tournament);
    }

    /** Finds free arenas in the tournament's pool and places any READY bracket matches onto them. */
    public void assignReadyMatches(Tournament tournament) {
        List<BracketMatch> ready = tournament.getReadyMatches();
        if (ready.isEmpty()) return;

        for (BracketMatch match : ready) {
            Arena freeArena = findFreeArena(tournament);
            if (freeArena == null) break; // no arenas free right now; will retry on next completion

            Player a = Bukkit.getPlayer(match.getPlayerA());
            Player b = Bukkit.getPlayer(match.getPlayerB());
            if (a == null || b == null) {
                // One of the players is offline; auto-forfeit to whoever is present, or skip
                handleOfflineForfeit(tournament, match, a, b);
                continue;
            }

            match.setStatus(BracketMatch.Status.IN_PROGRESS);
            match.setArenaId(freeArena.getId());
            arenaToMatch.put(freeArena.getId(), match);

            arenaManager.joinArena(freeArena, a);
            arenaManager.joinArena(freeArena, b);

            a.sendMessage("§6Your tournament match is starting now!");
            b.sendMessage("§6Your tournament match is starting now!");
        }
    }

    private void handleOfflineForfeit(Tournament tournament, BracketMatch match, Player a, Player b) {
        UUID winner = a != null ? match.getPlayerA() : (b != null ? match.getPlayerB() : null);
        if (winner == null) {
            return; // both offline, leave pending until one reconnects and admin retries
        }
        match.declareWinner(winner);
        broadcastToRegistered(tournament, "§eA player forfeited due to being offline; opponent advances.");
        if (!tournament.checkForChampion()) {
            assignReadyMatches(tournament);
        } else {
            announceChampion(tournament);
        }
    }

    private Arena findFreeArena(Tournament tournament) {
        for (String arenaId : tournament.getArenaPool()) {
            Arena arena = arenaManager.getArena(arenaId);
            if (arena != null && arena.getState() == ArenaState.WAITING) {
                return arena;
            }
        }
        return null;
    }

    /**
     * Call this when an arena running a tournament match finishes (winner determined).
     * Advances the bracket and tries to fill the now-free arena with the next ready match.
     */
    public void onArenaMatchFinished(Arena arena, UUID winnerUuid) {
        BracketMatch match = arenaToMatch.remove(arena.getId());
        if (match == null) return; // this arena wasn't running a tournament match

        Tournament tournament = findTournamentContaining(match);
        if (tournament == null) return;

        match.declareWinner(winnerUuid);
        broadcastToRegistered(tournament, "§aMatch result recorded. Bracket advancing...");

        if (tournament.checkForChampion()) {
            announceChampion(tournament);
        } else {
            assignReadyMatches(tournament);
        }
    }

    private Tournament findTournamentContaining(BracketMatch match) {
        for (Tournament t : tournaments.values()) {
            if (t.getAllMatches().contains(match)) return t;
        }
        return null;
    }

    private void announceChampion(Tournament tournament) {
        Player champion = Bukkit.getPlayer(tournament.getChampion());
        String name = champion != null ? champion.getName() : "Unknown";
        broadcastToRegistered(tournament, "§6§l" + name + " §6has won the tournament §e" + tournament.getId() + "§6!");
        Bukkit.broadcastMessage("§6§l" + name + " §6has won the tournament §e" + tournament.getId() + "§6!");
    }

    private void broadcastToRegistered(Tournament tournament, String message) {
        for (UUID uuid : tournament.getRegisteredPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }

    public void shutdown() {
        tournaments.clear();
        arenaToMatch.clear();
    }
}

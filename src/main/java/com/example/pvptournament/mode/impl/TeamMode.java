package com.example.pvptournament.mode.impl;

import com.example.pvptournament.arena.Arena;
import com.example.pvptournament.mode.GameMode;
import com.example.pvptournament.mode.GameModeType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/** Team deathmatch style: last team with a living player wins. */
public class TeamMode implements GameMode {

    @Override
    public GameModeType type() {
        return GameModeType.TEAM;
    }

    @Override
    public void onMatchStart(Arena arena) {
        List<String> teamKeys = new ArrayList<>(arena.getSpawnPoints().keySet());
        if (teamKeys.isEmpty()) teamKeys = List.of("red", "blue");

        List<UUID> players = new ArrayList<>(arena.getPlayers());
        int teamCount = teamKeys.size();
        Map<String, Integer> indexPerTeam = new HashMap<>();

        for (int i = 0; i < players.size(); i++) {
            Player p = Bukkit.getPlayer(players.get(i));
            if (p == null) continue;
            String team = teamKeys.get(i % teamCount);
            arena.getTeamAssignments().put(p.getUniqueId(), team);
            int idx = indexPerTeam.merge(team, 1, Integer::sum) - 1;

            ModeUtils.prepareForMatch(p);
            ModeUtils.teleportToSpawn(arena, p, team, idx);
            p.sendMessage("§eYou are on team §f" + team);
        }
    }

    @Override
    public boolean onPlayerDeath(Arena arena, Player victim, Player killer) {
        ModeUtils.makeSpectator(arena, victim);
        return checkWinCondition(arena);
    }

    @Override
    public boolean onPlayerLeave(Arena arena, Player player) {
        return checkWinCondition(arena);
    }

    private boolean checkWinCondition(Arena arena) {
        Set<String> aliveTeams = new HashSet<>();
        for (UUID uuid : arena.getPlayers()) {
            String team = arena.getTeamAssignments().get(uuid);
            if (team != null) aliveTeams.add(team);
        }
        if (aliveTeams.size() <= 1) {
            if (aliveTeams.size() == 1) {
                String winningTeam = aliveTeams.iterator().next();
                broadcastWinner(arena, winningTeam);
            }
            return true;
        }
        return false;
    }

    private void broadcastWinner(Arena arena, String team) {
        for (UUID uuid : arena.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage("§6Team " + team + " §ewins!");
        }
        for (UUID uuid : arena.getSpectators()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage("§6Team " + team + " §ewins!");
        }
    }

    @Override
    public void onMatchEnd(Arena arena) {
    }

    @Override
    public void resetArenaWorld(Arena arena) {
    }
}

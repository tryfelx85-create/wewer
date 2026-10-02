package com.example.pvptournament.mode.impl;

import com.example.pvptournament.arena.Arena;
import com.example.pvptournament.mode.GameMode;
import com.example.pvptournament.mode.GameModeType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Free-for-all: last player standing wins. */
public class FfaMode implements GameMode {

    @Override
    public GameModeType type() {
        return GameModeType.FFA;
    }

    @Override
    public void onMatchStart(Arena arena) {
        List<UUID> players = new ArrayList<>(arena.getPlayers());
        for (int i = 0; i < players.size(); i++) {
            Player p = Bukkit.getPlayer(players.get(i));
            if (p == null) continue;
            ModeUtils.prepareForMatch(p);
            ModeUtils.teleportToSpawn(arena, p, "solo", i);
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
        if (arena.getPlayers().size() <= 1) {
            if (arena.getPlayers().size() == 1) {
                UUID winnerUuid = arena.getPlayers().iterator().next();
                Player winner = Bukkit.getPlayer(winnerUuid);
                String name = winner != null ? winner.getName() : "Unknown";
                for (UUID uuid : arena.getSpectators()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) p.sendMessage("§6" + name + " §ewins the FFA!");
                }
                if (winner != null) winner.sendMessage("§6You won the FFA!");
            }
            return true;
        }
        return false;
    }

    @Override
    public void onMatchEnd(Arena arena) {
    }

    @Override
    public void resetArenaWorld(Arena arena) {
    }
}

package com.example.pvptournament.mode.impl;

import com.example.pvptournament.arena.Arena;
import com.example.pvptournament.mode.GameMode;
import com.example.pvptournament.mode.GameModeType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Simple 1v1: first death ends the match, survivor wins. */
public class DuelMode implements GameMode {

    @Override
    public GameModeType type() {
        return GameModeType.DUEL_1V1;
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
        if (killer != null) {
            arena.getPlayers().stream()
                    .map(Bukkit::getPlayer)
                    .filter(p -> p != null)
                    .forEach(p -> p.sendMessage("§6" + killer.getName() + " §ewins the duel!"));
        }
        return true; // one death always ends a duel
    }

    @Override
    public boolean onPlayerLeave(Arena arena, Player player) {
        return true; // opponent left, duel can't continue
    }

    @Override
    public void onMatchEnd(Arena arena) {
        // Winner already announced in onPlayerDeath; nothing else mode-specific needed.
    }

    @Override
    public void resetArenaWorld(Arena arena) {
        // No world changes in a vanilla duel arena by default.
    }
}

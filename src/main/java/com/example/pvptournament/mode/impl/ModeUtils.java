package com.example.pvptournament.mode.impl;

import com.example.pvptournament.PvPTournamentPlugin;
import com.example.pvptournament.arena.Arena;
import com.example.pvptournament.kit.Kit;
import com.example.pvptournament.kit.KitManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

final class ModeUtils {

    private ModeUtils() {
    }

    /** Teleports the player to a spawn point for the given team/slot key, cycling if more players than points. */
    static void teleportToSpawn(Arena arena, Player player, String teamKey, int indexInTeam) {
        List<Location> points = arena.getSpawnPoints().get(teamKey);
        if (points == null || points.isEmpty()) return;
        Location loc = points.get(indexInTeam % points.size());
        player.teleport(loc);
    }

    /** Resets player to survival, full health/food, clears inventory, then applies their selected kit. */
    static void prepareForMatch(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.setFireTicks(0);

        KitManager kitManager = PvPTournamentPlugin.get().getKitManager();
        UUID uuid = player.getUniqueId();
        Kit selected = kitManager.getSelectedKit(uuid);
        if (selected != null) {
            selected.applyTo(player);
        }
    }

    /** Switches a player to spectator mode and moves them to the arena's spectator spawn. */
    static void makeSpectator(Arena arena, Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        if (arena.getSpectatorSpawn() != null) {
            player.teleport(arena.getSpectatorSpawn());
        }
        arena.getPlayers().remove(player.getUniqueId());
        arena.addSpectator(player);
    }
}

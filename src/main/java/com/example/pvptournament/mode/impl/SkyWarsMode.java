package com.example.pvptournament.mode.impl;

import com.example.pvptournament.PvPTournamentPlugin;
import com.example.pvptournament.arena.Arena;
import com.example.pvptournament.mode.GameMode;
import com.example.pvptournament.mode.GameModeType;
import com.example.pvptournament.skywars.ChestRefillService;
import com.example.pvptournament.util.SchematicUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

/**
 * SkyWars: players spawn on separate pre-built islands (WorldEdit schematics),
 * loot chests, and fight. Border shrinks over time to force engagement.
 * Supports both solo and team variants via the same logic (teams share an island key).
 */
public class SkyWarsMode implements GameMode {

    private final PvPTournamentPlugin plugin;
    private final ChestRefillService chestRefillService;

    // Per-arena match clock, used for border shrink timing
    private final Map<String, Integer> matchSeconds = new HashMap<>();
    private static final int BORDER_SHRINK_START_SECONDS = 180; // start shrinking after 3 min
    private static final double BORDER_SHRINK_TARGET_SIZE = 20.0;
    private static final double BORDER_SHRINK_DURATION_SECONDS = 120;

    public SkyWarsMode(PvPTournamentPlugin plugin) {
        this.plugin = plugin;
        this.chestRefillService = new ChestRefillService(plugin);
    }

    @Override
    public GameModeType type() {
        return GameModeType.SKYWARS_SOLO; // handler is shared; arena.getModeType() decides actual behavior
    }

    @Override
    public void onMatchStart(Arena arena) {
        matchSeconds.put(arena.getId(), 0);

        // Paste fresh islands before placing players (ensures no leftover block damage from last match)
        pasteIslands(arena);

        boolean teamBased = arena.getModeType() == GameModeType.SKYWARS_TEAMS;
        List<String> islandKeys = new ArrayList<>(arena.getIslandSpawns().keySet());
        List<UUID> players = new ArrayList<>(arena.getPlayers());

        if (teamBased) {
            List<String> teamKeys = new ArrayList<>(arena.getSpawnPoints().keySet());
            int teamCount = Math.max(1, teamKeys.size());
            for (int i = 0; i < players.size(); i++) {
                Player p = Bukkit.getPlayer(players.get(i));
                if (p == null) continue;
                String team = teamKeys.get(i % teamCount);
                arena.getTeamAssignments().put(p.getUniqueId(), team);
                ModeUtils.prepareForMatch(p);
                Location islandLoc = arena.getIslandSpawns().get(team);
                if (islandLoc != null) p.teleport(islandLoc);
                p.sendMessage("§eYou are on team §f" + team);
            }
        } else {
            for (int i = 0; i < players.size() && i < islandKeys.size(); i++) {
                Player p = Bukkit.getPlayer(players.get(i));
                if (p == null) continue;
                ModeUtils.prepareForMatch(p);
                Location islandLoc = arena.getIslandSpawns().get(islandKeys.get(i));
                if (islandLoc != null) p.teleport(islandLoc);
            }
        }

        chestRefillService.refillAll(arena);

        if (arena.getWorld() != null) {
            WorldBorder border = arena.getWorld().getWorldBorder();
            // Reset to a generous default; real size should be set via admin command per-arena
            border.reset();
        }
    }

    private void pasteIslands(Arena arena) {
        if (arena.getSchematicName() == null || arena.getWorld() == null) return;
        File schematicFile = new File(SchematicUtil.schematicsFolder(plugin), arena.getSchematicName());
        for (Location islandOrigin : arena.getIslandSpawns().values()) {
            SchematicUtil.paste(plugin, schematicFile, islandOrigin);
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
        boolean teamBased = arena.getModeType() == GameModeType.SKYWARS_TEAMS;
        if (teamBased) {
            Set<String> aliveTeams = new HashSet<>();
            for (UUID uuid : arena.getPlayers()) {
                String team = arena.getTeamAssignments().get(uuid);
                if (team != null) aliveTeams.add(team);
            }
            if (aliveTeams.size() <= 1) {
                if (aliveTeams.size() == 1) {
                    announce(arena, "§6Team " + aliveTeams.iterator().next() + " §ewins SkyWars!");
                }
                return true;
            }
            return false;
        } else {
            if (arena.getPlayers().size() <= 1) {
                if (arena.getPlayers().size() == 1) {
                    Player winner = Bukkit.getPlayer(arena.getPlayers().iterator().next());
                    announce(arena, "§6" + (winner != null ? winner.getName() : "Unknown") + " §ewins SkyWars!");
                }
                return true;
            }
            return false;
        }
    }

    private void announce(Arena arena, String msg) {
        for (UUID uuid : arena.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(msg);
        }
        for (UUID uuid : arena.getSpectators()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(msg);
        }
    }

    @Override
    public void onTick(Arena arena) {
        int seconds = matchSeconds.merge(arena.getId(), 1, Integer::sum);
        if (arena.getWorld() == null) return;

        if (seconds == BORDER_SHRINK_START_SECONDS) {
            WorldBorder border = arena.getWorld().getWorldBorder();
            border.setSize(BORDER_SHRINK_TARGET_SIZE, (long) BORDER_SHRINK_DURATION_SECONDS);
            plugin.getArenaManager().broadcastToArena(arena, "§cThe border is shrinking!");
        }
    }

    @Override
    public void onMatchEnd(Arena arena) {
        matchSeconds.remove(arena.getId());
    }

    @Override
    public void resetArenaWorld(Arena arena) {
        if (arena.getWorld() != null) {
            arena.getWorld().getWorldBorder().reset();
        }
        chestRefillService.clearChests(arena);
        // Islands get fully re-pasted on next onMatchStart, so no incremental cleanup needed here.
    }

    public ChestRefillService getChestRefillService() {
        return chestRefillService;
    }
}

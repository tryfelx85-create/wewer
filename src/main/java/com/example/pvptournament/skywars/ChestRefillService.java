package com.example.pvptournament.skywars;

import com.example.pvptournament.PvPTournamentPlugin;
import com.example.pvptournament.arena.Arena;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Refills all chests within an arena's island area at match start/reset.
 * Chest locations are discovered once (cached) by scanning the schematic's
 * placed blocks, or can be manually registered via admin command.
 */
public class ChestRefillService {

    private final PvPTournamentPlugin plugin;
    private final SkyWarsLootTable lootTable = new SkyWarsLootTable();
    private final Map<String, List<Location>> arenaChests = new HashMap<>();

    public ChestRefillService(PvPTournamentPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerChest(Arena arena, Location location) {
        arenaChests.computeIfAbsent(arena.getId(), k -> new ArrayList<>()).add(location.clone());
    }

    public void clearChests(Arena arena) {
        arenaChests.remove(arena.getId());
    }

    /** Scans a cuboid region for chest blocks and caches their locations for this arena. */
    public void scanAndCacheChests(Arena arena, Location corner1, Location corner2) {
        List<Location> found = new ArrayList<>();
        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
        int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = corner1.getWorld().getBlockAt(x, y, z);
                    if (block.getState() instanceof Chest) {
                        found.add(block.getLocation());
                    }
                }
            }
        }
        arenaChests.put(arena.getId(), found);
        plugin.getLogger().info("Cached " + found.size() + " chests for arena " + arena.getId());
    }

    /** Clears and refills every cached chest for this arena with fresh random loot. */
    public void refillAll(Arena arena) {
        List<Location> chests = arenaChests.get(arena.getId());
        if (chests == null) return;
        for (Location loc : chests) {
            if (loc.getWorld() == null) continue;
            Block block = loc.getBlock();
            if (!(block.getState() instanceof Chest chest)) continue;
            chest.getInventory().clear();
            List<ItemStack> loot = lootTable.rollChestContents(3, 6);
            int size = chest.getInventory().getSize();
            Random random = new Random();
            for (ItemStack item : loot) {
                int slot = random.nextInt(size);
                // avoid overwriting; try a few times, else skip
                for (int attempt = 0; attempt < 5 && chest.getInventory().getItem(slot) != null; attempt++) {
                    slot = random.nextInt(size);
                }
                if (chest.getInventory().getItem(slot) == null) {
                    chest.getInventory().setItem(slot, item);
                }
            }
        }
    }
}

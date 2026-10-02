package com.example.pvptournament.skywars;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simple weighted loot table for SkyWars chest refills.
 * Expand this with config-driven entries later (YAML list of material/weight/min-max amount).
 */
public class SkyWarsLootTable {

    private record Entry(Material material, int weight, int minAmount, int maxAmount) {
    }

    private final List<Entry> entries = new ArrayList<>();
    private final Random random = new Random();

    public SkyWarsLootTable() {
        // Sensible defaults; override via config in production
        entries.add(new Entry(Material.IRON_SWORD, 5, 1, 1));
        entries.add(new Entry(Material.STONE_SWORD, 10, 1, 1));
        entries.add(new Entry(Material.BOW, 4, 1, 1));
        entries.add(new Entry(Material.ARROW, 10, 4, 12));
        entries.add(new Entry(Material.IRON_CHESTPLATE, 3, 1, 1));
        entries.add(new Entry(Material.LEATHER_CHESTPLATE, 8, 1, 1));
        entries.add(new Entry(Material.GOLDEN_APPLE, 3, 1, 2));
        entries.add(new Entry(Material.APPLE, 15, 2, 4));
        entries.add(new Entry(Material.COOKED_BEEF, 15, 2, 5));
        entries.add(new Entry(Material.OAK_PLANKS, 20, 8, 16));
        entries.add(new Entry(Material.COBWEB, 6, 1, 3));
        entries.add(new Entry(Material.ENDER_PEARL, 3, 1, 1));
        entries.add(new Entry(Material.SPLASH_POTION, 4, 1, 1));
    }

    private int totalWeight() {
        return entries.stream().mapToInt(Entry::weight).sum();
    }

    public ItemStack rollItem() {
        int roll = random.nextInt(totalWeight());
        int cumulative = 0;
        for (Entry entry : entries) {
            cumulative += entry.weight();
            if (roll < cumulative) {
                int amount = entry.minAmount() == entry.maxAmount()
                        ? entry.minAmount()
                        : entry.minAmount() + random.nextInt(entry.maxAmount() - entry.minAmount() + 1);
                return new ItemStack(entry.material(), amount);
            }
        }
        return new ItemStack(Material.STICK, 1); // fallback, unreachable in practice
    }

    /** Rolls between minItems and maxItems items for a single chest. */
    public List<ItemStack> rollChestContents(int minItems, int maxItems) {
        int count = minItems + random.nextInt(Math.max(1, maxItems - minItems + 1));
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(rollItem());
        }
        return items;
    }
}

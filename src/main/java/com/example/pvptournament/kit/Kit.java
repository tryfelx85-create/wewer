package com.example.pvptournament.kit;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * A reusable loadout: armor + hotbar/inventory items.
 * Built either via admin command (capturing current inventory) or edited through a GUI.
 */
public class Kit {

    private final String id;
    private String displayName;
    private List<ItemStack> contents = new ArrayList<>(); // size 36 (main inventory incl. hotbar)
    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;
    private String permission; // null = everyone can use it
    private long cooldownMillis = 0; // optional reuse cooldown for ability-kits later

    public Kit(String id) {
        this.id = id;
        this.displayName = id;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<ItemStack> getContents() {
        return contents;
    }

    public void setContents(List<ItemStack> contents) {
        this.contents = contents;
    }

    public ItemStack getHelmet() {
        return helmet;
    }

    public void setHelmet(ItemStack helmet) {
        this.helmet = helmet;
    }

    public ItemStack getChestplate() {
        return chestplate;
    }

    public void setChestplate(ItemStack chestplate) {
        this.chestplate = chestplate;
    }

    public ItemStack getLeggings() {
        return leggings;
    }

    public void setLeggings(ItemStack leggings) {
        this.leggings = leggings;
    }

    public ItemStack getBoots() {
        return boots;
    }

    public void setBoots(ItemStack boots) {
        this.boots = boots;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public long getCooldownMillis() {
        return cooldownMillis;
    }

    public void setCooldownMillis(long cooldownMillis) {
        this.cooldownMillis = cooldownMillis;
    }

    public boolean canUse(Player player) {
        return permission == null || player.hasPermission(permission);
    }

    /** Clears the player's inventory and applies this kit's items/armor. */
    public void applyTo(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        for (int i = 0; i < contents.size() && i < 36; i++) {
            ItemStack item = contents.get(i);
            if (item != null) inv.setItem(i, item.clone());
        }
        inv.setHelmet(helmet != null ? helmet.clone() : null);
        inv.setChestplate(chestplate != null ? chestplate.clone() : null);
        inv.setLeggings(leggings != null ? leggings.clone() : null);
        inv.setBoots(boots != null ? boots.clone() : null);
    }

    /** Captures the player's current inventory/armor into this kit (used by the admin "create kit" flow). */
    public void captureFrom(Player player) {
        PlayerInventory inv = player.getInventory();
        List<ItemStack> snapshot = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            snapshot.add(item != null ? item.clone() : null);
        }
        this.contents = snapshot;
        this.helmet = inv.getHelmet();
        this.chestplate = inv.getChestplate();
        this.leggings = inv.getLeggings();
        this.boots = inv.getBoots();
    }
}

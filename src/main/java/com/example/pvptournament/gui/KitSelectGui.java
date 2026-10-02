package com.example.pvptournament.gui;

import com.example.pvptournament.PvPTournamentPlugin;
import com.example.pvptournament.kit.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple inventory GUI listing all kits the player has permission for.
 * Click handling lives in listener.GuiListener, matched by inventory title.
 */
public class KitSelectGui {

    public static final String TITLE = "Select a Kit";

    // slot -> kit id, rebuilt each time open() is called (per-player permission filtering)
    private final Map<Integer, String> slotToKitId = new LinkedHashMap<>();

    public Inventory build(Player player) {
        PvPTournamentPlugin plugin = PvPTournamentPlugin.get();
        Inventory inv = org.bukkit.Bukkit.createInventory(null, 27, Component.text(TITLE));

        int slot = 0;
        for (Kit kit : plugin.getKitManager().getKits()) {
            if (!kit.canUse(player)) continue;
            if (slot >= 27) break;

            ItemStack icon = new ItemStack(Material.IRON_SWORD);
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(Component.text(kit.getDisplayName(), NamedTextColor.GOLD));
            icon.setItemMeta(meta);

            inv.setItem(slot, icon);
            slotToKitId.put(slot, kit.getId());
            slot++;
        }
        return inv;
    }

    public String getKitIdAt(int slot) {
        return slotToKitId.get(slot);
    }
}

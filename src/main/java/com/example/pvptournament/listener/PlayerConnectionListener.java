package com.example.pvptournament.listener;

import com.example.pvptournament.PvPTournamentPlugin;
import com.example.pvptournament.arena.Arena;
import com.example.pvptournament.gui.KitSelectGui;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

public class PlayerConnectionListener implements Listener {

    private final PvPTournamentPlugin plugin;

    public PlayerConnectionListener(PvPTournamentPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        plugin.getQueueManager().leaveQueue(player);

        findArenaOf(player).ifPresent(arena -> {
            if (arena.getPlayers().contains(player.getUniqueId())) {
                plugin.getArenaManager().leaveArena(arena, player);
            } else if (arena.getSpectators().contains(player.getUniqueId())) {
                arena.removeSpectator(player);
            }
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().title() == null) return;
        Component title = event.getView().title();
        // Match by the plain-text title we set when building the GUI
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!titleMatches(title, KitSelectGui.TITLE)) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        // Re-derive which kit id was in this slot by rebuilding is wasteful; in production,
        // store a per-open-session KitSelectGui instance keyed by player UUID instead.
        // For this scaffold we re-resolve by icon display name match as a simple placeholder.
        var clicked = event.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) return;
        String displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(clicked.getItemMeta().displayName() != null
                        ? clicked.getItemMeta().displayName()
                        : Component.empty());

        plugin.getKitManager().getKits().stream()
                .filter(k -> k.getDisplayName().equals(displayName))
                .findFirst()
                .ifPresent(kit -> {
                    plugin.getKitManager().setSelectedKit(player.getUniqueId(), kit.getId());
                    player.sendMessage("§aSelected kit: §f" + kit.getDisplayName());
                    player.closeInventory();
                });
    }

    private boolean titleMatches(Component title, String plain) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(title).equals(plain);
    }

    private Optional<Arena> findArenaOf(Player player) {
        return plugin.getArenaManager().getArenas().stream()
                .filter(a -> a.getPlayers().contains(player.getUniqueId())
                        || a.getSpectators().contains(player.getUniqueId()))
                .findFirst();
    }
}

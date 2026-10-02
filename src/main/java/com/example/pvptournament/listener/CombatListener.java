package com.example.pvptournament.listener;

import com.example.pvptournament.PvPTournamentPlugin;
import com.example.pvptournament.arena.Arena;
import com.example.pvptournament.arena.ArenaManager;
import com.example.pvptournament.arena.ArenaState;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Optional;

public class CombatListener implements Listener {

    private final PvPTournamentPlugin plugin;

    public CombatListener(PvPTournamentPlugin plugin) {
        this.plugin = plugin;
    }

    /** Blocks PvP damage between players who are not both currently inside the same in-progress arena. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        Optional<Arena> victimArena = findArenaOf(victim);
        Optional<Arena> attackerArena = findArenaOf(attacker);

        boolean sameLiveArena = victimArena.isPresent() && attackerArena.isPresent()
                && victimArena.get() == attackerArena.get()
                && victimArena.get().getState() == ArenaState.INGAME;

        if (!sameLiveArena) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Optional<Arena> arenaOpt = findArenaOf(victim);
        if (arenaOpt.isEmpty()) return;

        Arena arena = arenaOpt.get();
        if (arena.getState() != ArenaState.INGAME) return;

        Player killer = victim.getKiller();

        // Keep death light: no item drops / inventory clear mid-tournament, custom death message
        event.setCancelled(false);
        event.deathMessage(null);
        arena.getWorld();
        plugin.getServer().broadcast(
                Component.text((killer != null
                        ? victim.getName() + " was eliminated by " + killer.getName()
                        : victim.getName() + " died")),
                "pvpt.use"
        );

        ArenaManager arenaManager = plugin.getArenaManager();
        arenaManager.handleDeath(arena, victim, killer);

        // If this ended the match (checked via mode handler's return in handleDeath -> endMatch),
        // route result into the tournament system when applicable.
        if (arena.getState() == ArenaState.ENDING && arena.getPlayers().size() == 1) {
            var winnerUuid = arena.getPlayers().iterator().next();
            plugin.getTournamentManager().onArenaMatchFinished(arena, winnerUuid);
        }
    }

    private Optional<Arena> findArenaOf(Player player) {
        return plugin.getArenaManager().getArenas().stream()
                .filter(a -> a.getPlayers().contains(player.getUniqueId())
                        || a.getSpectators().contains(player.getUniqueId()))
                .findFirst();
    }
}

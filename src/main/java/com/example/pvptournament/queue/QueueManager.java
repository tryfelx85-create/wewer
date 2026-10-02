package com.example.pvptournament.queue;

import com.example.pvptournament.PvPTournamentPlugin;
import com.example.pvptournament.arena.Arena;
import com.example.pvptournament.arena.ArenaManager;
import com.example.pvptournament.mode.GameModeType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Casual matchmaking: players queue for a GameModeType, get auto-assigned to the first
 * joinable arena of that type, or wait until one becomes available.
 */
public class QueueManager {

    private final PvPTournamentPlugin plugin;
    private final ArenaManager arenaManager;

    // modeType -> queued player UUIDs (FIFO-ish via LinkedHashSet)
    private final Map<GameModeType, Set<UUID>> queues = new EnumMap<>(GameModeType.class);
    private final Map<UUID, GameModeType> playerQueueMode = new ConcurrentHashMap<>();

    public QueueManager(PvPTournamentPlugin plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        for (GameModeType type : GameModeType.values()) {
            queues.put(type, new LinkedHashSet<>());
        }
        // Periodically attempt to drain queues into available arenas
        Bukkit.getScheduler().runTaskTimer(plugin, this::tryDrainAll, 40L, 40L);
    }

    public boolean joinQueue(Player player, GameModeType type) {
        if (playerQueueMode.containsKey(player.getUniqueId())) {
            return false; // already queued somewhere
        }
        queues.get(type).add(player.getUniqueId());
        playerQueueMode.put(player.getUniqueId(), type);
        player.sendMessage("§aJoined queue for §f" + type.name() + "§a. Players in queue: "
                + queues.get(type).size());
        tryDrain(type);
        return true;
    }

    public void leaveQueue(Player player) {
        GameModeType type = playerQueueMode.remove(player.getUniqueId());
        if (type != null) {
            queues.get(type).remove(player.getUniqueId());
            player.sendMessage("§eLeft the queue.");
        }
    }

    public boolean isQueued(Player player) {
        return playerQueueMode.containsKey(player.getUniqueId());
    }

    private void tryDrainAll() {
        for (GameModeType type : GameModeType.values()) {
            tryDrain(type);
        }
    }

    /** Pulls queued players into any joinable arena of this type until the queue or arena space runs out. */
    private void tryDrain(GameModeType type) {
        Set<UUID> queue = queues.get(type);
        if (queue.isEmpty()) return;

        List<Arena> available = arenaManager.findAvailable(type);
        for (Arena arena : available) {
            Iterator<UUID> it = queue.iterator();
            while (it.hasNext() && !arena.isFull()) {
                UUID uuid = it.next();
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) {
                    it.remove();
                    playerQueueMode.remove(uuid);
                    continue;
                }
                if (arenaManager.joinArena(arena, player)) {
                    it.remove();
                    playerQueueMode.remove(uuid);
                    player.sendMessage("§aMatch found! Teleporting you to arena §f" + arena.getId());
                }
            }
            if (queue.isEmpty()) break;
        }
    }

    public void shutdown() {
        queues.values().forEach(Set::clear);
        playerQueueMode.clear();
    }
}

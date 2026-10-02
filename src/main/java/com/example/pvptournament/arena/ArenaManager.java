package com.example.pvptournament.arena;

import com.example.pvptournament.PvPTournamentPlugin;
import com.example.pvptournament.mode.GameMode;
import com.example.pvptournament.mode.GameModeType;
import com.example.pvptournament.mode.impl.DuelMode;
import com.example.pvptournament.mode.impl.FfaMode;
import com.example.pvptournament.mode.impl.SkyWarsMode;
import com.example.pvptournament.mode.impl.TeamMode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Owns the lifecycle of every Arena: loading from disk, running the
 * WAITING -> STARTING -> INGAME -> ENDING -> RESETTING state machine,
 * and delegating mode-specific behavior to the matching GameMode.
 */
public class ArenaManager {

    private final PvPTournamentPlugin plugin;
    private final File arenasFolder;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private final Map<String, BukkitTask> countdownTasks = new HashMap<>();
    private final Map<String, BukkitTask> tickTasks = new HashMap<>();
    private final Map<GameModeType, GameMode> modeHandlers = new EnumMap<>(GameModeType.class);

    public ArenaManager(PvPTournamentPlugin plugin) {
        this.plugin = plugin;
        this.arenasFolder = new File(plugin.getDataFolder(), "arenas");
        if (!arenasFolder.exists()) arenasFolder.mkdirs();

        registerModeHandlers();
    }

    private void registerModeHandlers() {
        modeHandlers.put(GameModeType.DUEL_1V1, new DuelMode());
        modeHandlers.put(GameModeType.TEAM, new TeamMode());
        modeHandlers.put(GameModeType.FFA, new FfaMode());
        SkyWarsMode skyWarsMode = new SkyWarsMode(plugin);
        modeHandlers.put(GameModeType.SKYWARS_SOLO, skyWarsMode);
        modeHandlers.put(GameModeType.SKYWARS_TEAMS, skyWarsMode);
    }

    public GameMode getHandler(GameModeType type) {
        return modeHandlers.get(type);
    }

    // ---------- Persistence ----------

    public void loadAll() {
        arenas.clear();
        File[] files = arenasFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            try {
                Arena arena = loadArena(file);
                arenas.put(arena.getId(), arena);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to load arena file " + file.getName() + ": " + ex.getMessage());
            }
        }
    }

    private Arena loadArena(File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String id = cfg.getString("id", file.getName().replace(".yml", ""));
        GameModeType type = GameModeType.valueOf(cfg.getString("mode", "FFA"));
        Arena arena = new Arena(id, type);

        arena.setMinPlayers(cfg.getInt("min-players", type.minPlayers()));
        arena.setMaxPlayers(cfg.getInt("max-players", type.maxPlayers()));
        arena.setCountdownSeconds(cfg.getInt("countdown-seconds", 15));
        arena.setSchematicName(cfg.getString("schematic", null));

        String worldName = cfg.getString("world");
        if (worldName != null) {
            arena.setWorld(Bukkit.getWorld(worldName));
        }

        if (cfg.contains("lobby-spawn")) {
            arena.setLobbySpawn(deserializeLocation(cfg.getConfigurationSection("lobby-spawn")));
        }
        if (cfg.contains("spectator-spawn")) {
            arena.setSpectatorSpawn(deserializeLocation(cfg.getConfigurationSection("spectator-spawn")));
        }

        ConfigurationSection spawnsSection = cfg.getConfigurationSection("spawn-points");
        if (spawnsSection != null) {
            for (String team : spawnsSection.getKeys(false)) {
                List<?> rawList = spawnsSection.getList(team);
                List<Location> locs = new ArrayList<>();
                if (rawList != null) {
                    for (Object o : rawList) {
                        if (o instanceof ConfigurationSection section) {
                            locs.add(deserializeLocation(section));
                        }
                    }
                }
                arena.getSpawnPoints().put(team, locs);
            }
        }

        ConfigurationSection islandSection = cfg.getConfigurationSection("island-spawns");
        if (islandSection != null) {
            for (String key : islandSection.getKeys(false)) {
                arena.getIslandSpawns().put(key, deserializeLocation(islandSection.getConfigurationSection(key)));
            }
        }

        return arena;
    }

    public void saveArena(Arena arena) {
        File file = new File(arenasFolder, arena.getId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("id", arena.getId());
        cfg.set("mode", arena.getModeType().name());
        cfg.set("min-players", arena.getMinPlayers());
        cfg.set("max-players", arena.getMaxPlayers());
        cfg.set("countdown-seconds", arena.getCountdownSeconds());
        if (arena.getSchematicName() != null) cfg.set("schematic", arena.getSchematicName());
        if (arena.getWorld() != null) cfg.set("world", arena.getWorld().getName());
        if (arena.getLobbySpawn() != null) serializeLocation(cfg.createSection("lobby-spawn"), arena.getLobbySpawn());
        if (arena.getSpectatorSpawn() != null)
            serializeLocation(cfg.createSection("spectator-spawn"), arena.getSpectatorSpawn());

        ConfigurationSection spawnsSection = cfg.createSection("spawn-points");
        for (var entry : arena.getSpawnPoints().entrySet()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Location loc : entry.getValue()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("world", loc.getWorld().getName());
                map.put("x", loc.getX());
                map.put("y", loc.getY());
                map.put("z", loc.getZ());
                map.put("yaw", loc.getYaw());
                map.put("pitch", loc.getPitch());
                list.add(map);
            }
            spawnsSection.set(entry.getKey(), list);
        }

        ConfigurationSection islandSection = cfg.createSection("island-spawns");
        for (var entry : arena.getIslandSpawns().entrySet()) {
            serializeLocation(islandSection.createSection(entry.getKey()), entry.getValue());
        }

        try {
            cfg.save(file);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to save arena " + arena.getId() + ": " + ex.getMessage());
        }
    }

    private Location deserializeLocation(ConfigurationSection section) {
        if (section == null) return null;
        return new Location(
                Bukkit.getWorld(section.getString("world")),
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch")
        );
    }

    private void serializeLocation(ConfigurationSection section, Location loc) {
        section.set("world", loc.getWorld().getName());
        section.set("x", loc.getX());
        section.set("y", loc.getY());
        section.set("z", loc.getZ());
        section.set("yaw", (double) loc.getYaw());
        section.set("pitch", (double) loc.getPitch());
    }

    // ---------- Access ----------

    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    public Arena getArena(String id) {
        return arenas.get(id);
    }

    public void registerArena(Arena arena) {
        arenas.put(arena.getId(), arena);
        saveArena(arena);
    }

    public void deleteArena(String id) {
        arenas.remove(id);
        File file = new File(arenasFolder, id + ".yml");
        if (file.exists()) file.delete();
    }

    public List<Arena> findAvailable(GameModeType type) {
        return arenas.values().stream()
                .filter(a -> a.getModeType() == type)
                .filter(Arena::isJoinable)
                .collect(Collectors.toList());
    }

    // ---------- Lifecycle ----------

    /** Attempts to add a player to an arena and starts countdown if min players reached. */
    public boolean joinArena(Arena arena, Player player) {
        if (!arena.isJoinable()) return false;
        arena.addPlayer(player);
        if (arena.getLobbySpawn() != null) {
            player.teleport(arena.getLobbySpawn());
        }
        if (arena.getState() == ArenaState.WAITING && arena.hasEnoughToStart()) {
            startCountdown(arena);
        }
        if (arena.isFull() && arena.getState() == ArenaState.STARTING) {
            // Instantly begin if full, skip remaining countdown
            cancelCountdown(arena);
            beginMatch(arena);
        }
        return true;
    }

    public void leaveArena(Arena arena, Player player) {
        arena.removePlayer(player);
        GameMode handler = modeHandlers.get(arena.getModeType());
        if (arena.getState() == ArenaState.INGAME && handler != null) {
            boolean shouldEnd = handler.onPlayerLeave(arena, player);
            if (shouldEnd) {
                endMatch(arena);
            }
        } else if (arena.getState() == ArenaState.STARTING && !arena.hasEnoughToStart()) {
            cancelCountdown(arena);
            arena.setState(ArenaState.WAITING);
        }
    }

    private void startCountdown(Arena arena) {
        arena.setState(ArenaState.STARTING);
        final int[] secondsLeft = {arena.getCountdownSeconds()};
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (secondsLeft[0] <= 0) {
                cancelCountdown(arena);
                beginMatch(arena);
                return;
            }
            if (secondsLeft[0] <= 5 || secondsLeft[0] % 10 == 0) {
                broadcastToArena(arena, "§eMatch starting in §c" + secondsLeft[0] + "§e...");
            }
            secondsLeft[0]--;
        }, 0L, 20L);
        countdownTasks.put(arena.getId(), task);
    }

    private void cancelCountdown(Arena arena) {
        BukkitTask task = countdownTasks.remove(arena.getId());
        if (task != null) task.cancel();
    }

    private void beginMatch(Arena arena) {
        if (!arena.hasEnoughToStart()) {
            arena.setState(ArenaState.WAITING);
            return;
        }
        arena.setState(ArenaState.INGAME);
        GameMode handler = modeHandlers.get(arena.getModeType());
        if (handler != null) {
            handler.onMatchStart(arena);
        }
        startTickLoop(arena);
    }

    private void startTickLoop(Arena arena) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            GameMode handler = modeHandlers.get(arena.getModeType());
            if (handler != null && arena.getState() == ArenaState.INGAME) {
                handler.onTick(arena);
            }
        }, 20L, 20L);
        tickTasks.put(arena.getId(), task);
    }

    private void stopTickLoop(Arena arena) {
        BukkitTask task = tickTasks.remove(arena.getId());
        if (task != null) task.cancel();
    }

    /** Called by combat listener on PlayerDeathEvent for players inside an arena. */
    public void handleDeath(Arena arena, Player victim, Player killer) {
        GameMode handler = modeHandlers.get(arena.getModeType());
        if (handler == null) return;
        boolean shouldEnd = handler.onPlayerDeath(arena, victim, killer);
        if (shouldEnd) {
            endMatch(arena);
        }
    }

    public void endMatch(Arena arena) {
        if (arena.getState() != ArenaState.INGAME) return;
        arena.setState(ArenaState.ENDING);
        stopTickLoop(arena);
        GameMode handler = modeHandlers.get(arena.getModeType());
        if (handler != null) {
            handler.onMatchEnd(arena);
        }
        // Give players a short delay to see results before reset
        Bukkit.getScheduler().runTaskLater(plugin, () -> resetArena(arena), 100L);
    }

    private void resetArena(Arena arena) {
        arena.setState(ArenaState.RESETTING);
        GameMode handler = modeHandlers.get(arena.getModeType());
        if (handler != null) {
            handler.resetArenaWorld(arena);
        }
        // Return all players/spectators to lobby/hub (hook your hub teleport here)
        for (UUID uuid : new HashSet<>(arena.getPlayers())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && arena.getLobbySpawn() != null) {
                p.teleport(arena.getLobbySpawn());
            }
        }
        for (UUID uuid : new HashSet<>(arena.getSpectators())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && arena.getLobbySpawn() != null) {
                p.teleport(arena.getLobbySpawn());
            }
        }
        arena.resetRuntimeState();
    }

    public void broadcastToArena(Arena arena, String message) {
        for (UUID uuid : arena.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
        for (UUID uuid : arena.getSpectators()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }

    public void shutdown() {
        countdownTasks.values().forEach(BukkitTask::cancel);
        tickTasks.values().forEach(BukkitTask::cancel);
        arenas.values().forEach(this::saveArena);
    }
}

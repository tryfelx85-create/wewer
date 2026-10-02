package com.example.pvptournament.arena;

import com.example.pvptournament.mode.GameModeType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a single playable arena instance. One Arena = one active match at a time.
 * Supports whichever GameModeType it was configured for.
 */
public class Arena {

    private final String id;
    private GameModeType modeType;
    private ArenaState state = ArenaState.WAITING;

    // Spawn points: team name -> list of locations (solo modes use team "solo")
    private final Map<String, List<Location>> spawnPoints = new LinkedHashMap<>();

    // For SkyWars: island center points per team/player slot
    private final Map<String, Location> islandSpawns = new LinkedHashMap<>();

    private Location lobbySpawn;
    private Location spectatorSpawn;
    private World world;

    private String schematicName; // SkyWars pre-built island schematic
    private int countdownSeconds = 15;
    private int minPlayers;
    private int maxPlayers;

    private final Set<UUID> players = ConcurrentHashMap.newKeySet();
    private final Set<UUID> spectators = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> teamAssignments = new ConcurrentHashMap<>();

    public Arena(String id, GameModeType modeType) {
        this.id = id;
        this.modeType = modeType;
        this.minPlayers = modeType.minPlayers();
        this.maxPlayers = modeType.maxPlayers();
    }

    public String getId() {
        return id;
    }

    public GameModeType getModeType() {
        return modeType;
    }

    public void setModeType(GameModeType modeType) {
        this.modeType = modeType;
    }

    public ArenaState getState() {
        return state;
    }

    public void setState(ArenaState state) {
        this.state = state;
    }

    public Map<String, List<Location>> getSpawnPoints() {
        return spawnPoints;
    }

    public Map<String, Location> getIslandSpawns() {
        return islandSpawns;
    }

    public Location getLobbySpawn() {
        return lobbySpawn;
    }

    public void setLobbySpawn(Location lobbySpawn) {
        this.lobbySpawn = lobbySpawn;
    }

    public Location getSpectatorSpawn() {
        return spectatorSpawn;
    }

    public void setSpectatorSpawn(Location spectatorSpawn) {
        this.spectatorSpawn = spectatorSpawn;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public String getSchematicName() {
        return schematicName;
    }

    public void setSchematicName(String schematicName) {
        this.schematicName = schematicName;
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public void setCountdownSeconds(int countdownSeconds) {
        this.countdownSeconds = countdownSeconds;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public Set<UUID> getPlayers() {
        return players;
    }

    public Set<UUID> getSpectators() {
        return spectators;
    }

    public Map<UUID, String> getTeamAssignments() {
        return teamAssignments;
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public boolean hasEnoughToStart() {
        return players.size() >= minPlayers;
    }

    public boolean isJoinable() {
        return (state == ArenaState.WAITING || state == ArenaState.STARTING) && !isFull();
    }

    public boolean isInUse() {
        return state == ArenaState.STARTING || state == ArenaState.INGAME || state == ArenaState.ENDING || state == ArenaState.RESETTING;
    }

    public void addPlayer(Player player) {
        players.add(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
        teamAssignments.remove(player.getUniqueId());
    }

    public void addSpectator(Player player) {
        spectators.add(player.getUniqueId());
    }

    public void removeSpectator(Player player) {
        spectators.remove(player.getUniqueId());
    }

    /** Clears all runtime player/team state. Call on reset, does not touch config. */
    public void resetRuntimeState() {
        players.clear();
        spectators.clear();
        teamAssignments.clear();
        state = ArenaState.WAITING;
    }
}

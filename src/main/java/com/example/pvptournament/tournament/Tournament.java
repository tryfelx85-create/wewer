package com.example.pvptournament.tournament;

import com.example.pvptournament.mode.GameModeType;

import java.util.*;

public class Tournament {

    public enum Format {
        SINGLE_ELIMINATION,
        DOUBLE_ELIMINATION
    }

    public enum Phase {
        REGISTRATION, // players can /tournament join
        IN_PROGRESS,  // bracket generated, matches being played
        COMPLETE
    }

    private final String id;
    private final Format format;
    private final GameModeType modeType; // which arena type to pull matches from
    private final List<String> arenaPool; // arena ids usable for this tournament's matches

    private final Set<UUID> registeredPlayers = new LinkedHashSet<>();
    private List<BracketMatch> allMatches = new ArrayList<>();
    private BracketBuilder.DoubleElimResult doubleElimResult; // non-null only if format is double-elim

    private Phase phase = Phase.REGISTRATION;
    private UUID champion;

    public Tournament(String id, Format format, GameModeType modeType, List<String> arenaPool) {
        this.id = id;
        this.format = format;
        this.modeType = modeType;
        this.arenaPool = arenaPool;
    }

    public String getId() {
        return id;
    }

    public Format getFormat() {
        return format;
    }

    public GameModeType getModeType() {
        return modeType;
    }

    public List<String> getArenaPool() {
        return arenaPool;
    }

    public Set<UUID> getRegisteredPlayers() {
        return registeredPlayers;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public UUID getChampion() {
        return champion;
    }

    public void setChampion(UUID champion) {
        this.champion = champion;
    }

    public List<BracketMatch> getAllMatches() {
        return allMatches;
    }

    public boolean register(UUID player) {
        if (phase != Phase.REGISTRATION) return false;
        return registeredPlayers.add(player);
    }

    public boolean unregister(UUID player) {
        if (phase != Phase.REGISTRATION) return false;
        return registeredPlayers.remove(player);
    }

    /** Builds the bracket from currently registered players and moves to IN_PROGRESS. */
    public void startBracket() {
        if (format == Format.SINGLE_ELIMINATION) {
            allMatches = BracketBuilder.buildSingleElimination(new ArrayList<>(registeredPlayers));
        } else {
            doubleElimResult = BracketBuilder.buildDoubleElimination(new ArrayList<>(registeredPlayers));
            allMatches = doubleElimResult.allMatches();
        }
        phase = Phase.IN_PROGRESS;
    }

    /** Returns all matches currently READY to be started (both players known, not yet played). */
    public List<BracketMatch> getReadyMatches() {
        return allMatches.stream()
                .filter(m -> m.getStatus() == BracketMatch.Status.READY)
                .toList();
    }

    /** True once a single champion is decided (grand final complete, or sole remaining single-elim winner). */
    public boolean checkForChampion() {
        if (format == Format.SINGLE_ELIMINATION) {
            BracketMatch finalMatch = allMatches.get(allMatches.size() - 1);
            if (finalMatch.getStatus() == BracketMatch.Status.COMPLETE) {
                champion = finalMatch.getWinner();
                phase = Phase.COMPLETE;
                return true;
            }
        } else if (doubleElimResult != null) {
            BracketMatch grandFinal = doubleElimResult.grandFinal();
            if (grandFinal.getStatus() == BracketMatch.Status.COMPLETE) {
                champion = grandFinal.getWinner();
                phase = Phase.COMPLETE;
                return true;
            }
        }
        return false;
    }
}

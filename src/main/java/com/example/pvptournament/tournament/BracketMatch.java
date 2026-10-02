package com.example.pvptournament.tournament;

import java.util.UUID;

/**
 * A single bracket slot. Either playerA/playerB (1v1 bracket) and eventually
 * the winner propagates to nextMatch's slot A or B.
 * For double elimination, losers get routed to a parallel loser-bracket match (set externally).
 */
public class BracketMatch {

    public enum Status {
        PENDING,     // waiting on one or both players to be decided by earlier matches
        READY,       // both players known, waiting for admin to start
        IN_PROGRESS, // currently being played in an arena
        COMPLETE     // winner decided
    }

    private final int round;
    private final int matchIndex; // position within round, used for bracket layout

    private UUID playerA;
    private UUID playerB;
    private UUID winner;
    private String arenaId; // arena currently/last used for this match

    private Status status = Status.PENDING;

    // Bracket linkage: where does the winner (and loser, for double-elim) go next
    private BracketMatch nextMatchWinner;
    private boolean nextMatchWinnerSlotA; // true = winner fills slot A of next match
    private BracketMatch nextMatchLoser;
    private boolean nextMatchLoserSlotA;

    public BracketMatch(int round, int matchIndex) {
        this.round = round;
        this.matchIndex = matchIndex;
    }

    public int getRound() {
        return round;
    }

    public int getMatchIndex() {
        return matchIndex;
    }

    public UUID getPlayerA() {
        return playerA;
    }

    public UUID getPlayerB() {
        return playerB;
    }

    public UUID getWinner() {
        return winner;
    }

    public String getArenaId() {
        return arenaId;
    }

    public void setArenaId(String arenaId) {
        this.arenaId = arenaId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public BracketMatch getNextMatchWinner() {
        return nextMatchWinner;
    }

    public BracketMatch getNextMatchLoser() {
        return nextMatchLoser;
    }

    public void linkWinnerTo(BracketMatch next, boolean slotA) {
        this.nextMatchWinner = next;
        this.nextMatchWinnerSlotA = slotA;
    }

    public void linkLoserTo(BracketMatch next, boolean slotA) {
        this.nextMatchLoser = next;
        this.nextMatchLoserSlotA = slotA;
    }

    public void setPlayerA(UUID playerA) {
        this.playerA = playerA;
        updateReadiness();
    }

    public void setPlayerB(UUID playerB) {
        this.playerB = playerB;
        updateReadiness();
    }

    private void updateReadiness() {
        if (playerA != null && playerB != null && status == Status.PENDING) {
            status = Status.READY;
        }
        // Bye handling: if one slot will never be filled (e.g. odd bracket), caller should
        // auto-advance via BracketManager rather than leaving this match stuck.
    }

    public boolean isBye() {
        return (playerA == null) != (playerB == null); // exactly one side is null
    }

    /** Records the result and propagates winner/loser to linked next matches. */
    public UUID declareWinner(UUID winnerUuid) {
        this.winner = winnerUuid;
        this.status = Status.COMPLETE;

        UUID loser = winnerUuid.equals(playerA) ? playerB : playerA;

        if (nextMatchWinner != null) {
            if (nextMatchWinnerSlotA) nextMatchWinner.setPlayerA(winnerUuid);
            else nextMatchWinner.setPlayerB(winnerUuid);
        }
        if (nextMatchLoser != null && loser != null) {
            if (nextMatchLoserSlotA) nextMatchLoser.setPlayerA(loser);
            else nextMatchLoser.setPlayerB(loser);
        }
        return loser;
    }
}

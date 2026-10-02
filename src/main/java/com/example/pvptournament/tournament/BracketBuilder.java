package com.example.pvptournament.tournament;

import java.util.*;

/**
 * Generates single-elimination or double-elimination bracket structures from a
 * list of entrant UUIDs. Handles non-power-of-two entrant counts via byes
 * (a bye auto-advances the sole player without a match).
 */
public final class BracketBuilder {

    private BracketBuilder() {
    }

    /** Returns all matches created, in round order. Round 1 matches are entry points. */
    public static List<BracketMatch> buildSingleElimination(List<UUID> entrants) {
        List<UUID> players = new ArrayList<>(entrants);
        Collections.shuffle(players); // random seeding; swap for seeded logic later if desired

        int size = nextPowerOfTwo(players.size());
        while (players.size() < size) {
            players.add(null); // null = bye slot
        }

        List<BracketMatch> allMatches = new ArrayList<>();
        List<BracketMatch> currentRound = new ArrayList<>();

        int round = 1;
        for (int i = 0; i < players.size(); i += 2) {
            BracketMatch match = new BracketMatch(round, i / 2);
            match.setPlayerA(players.get(i));
            match.setPlayerB(players.get(i + 1));
            currentRound.add(match);
            allMatches.add(match);
        }

        // Build subsequent rounds, linking winners forward
        while (currentRound.size() > 1) {
            round++;
            List<BracketMatch> nextRound = new ArrayList<>();
            for (int i = 0; i < currentRound.size(); i += 2) {
                BracketMatch next = new BracketMatch(round, i / 2);
                currentRound.get(i).linkWinnerTo(next, true);
                currentRound.get(i + 1).linkWinnerTo(next, false);
                nextRound.add(next);
                allMatches.add(next);
            }
            currentRound = nextRound;
        }

        // Auto-resolve byes in round 1 (single player, no opponent -> auto win)
        resolveByes(allMatches);

        return allMatches;
    }

    /**
     * Double elimination: winners bracket feeds a losers bracket; losers bracket
     * survivor faces winners bracket champion in a grand final.
     * This is a simplified standard double-elim construction.
     */
    public static DoubleElimResult buildDoubleElimination(List<UUID> entrants) {
        List<BracketMatch> winnersBracket = buildSingleElimination(entrants);

        // Build losers bracket shell sized to accept drop-downs from winners bracket.
        // Simplified approach: one losers match per winners-round-pair, linear chain.
        List<BracketMatch> losersBracket = new ArrayList<>();
        int losersRound = 1;
        BracketMatch previousLosersMatch = null;

        // Group winners matches by round
        Map<Integer, List<BracketMatch>> byRound = new TreeMap<>();
        for (BracketMatch m : winnersBracket) {
            byRound.computeIfAbsent(m.getRound(), r -> new ArrayList<>()).add(m);
        }

        for (var entry : byRound.entrySet()) {
            List<BracketMatch> roundMatches = entry.getValue();
            for (BracketMatch wm : roundMatches) {
                BracketMatch losersMatch = new BracketMatch(100 + losersRound, losersBracket.size());
                if (previousLosersMatch != null) {
                    previousLosersMatch.linkWinnerTo(losersMatch, true);
                }
                wm.linkLoserTo(losersMatch, previousLosersMatch == null);
                losersBracket.add(losersMatch);
                previousLosersMatch = losersMatch;
                losersRound++;
            }
        }

        BracketMatch winnersFinal = winnersBracket.get(winnersBracket.size() - 1);
        BracketMatch losersFinal = losersBracket.isEmpty() ? null : losersBracket.get(losersBracket.size() - 1);

        BracketMatch grandFinal = new BracketMatch(999, 0);
        winnersFinal.linkWinnerTo(grandFinal, true);
        if (losersFinal != null) {
            losersFinal.linkWinnerTo(grandFinal, false);
        }

        resolveByes(winnersBracket);

        List<BracketMatch> all = new ArrayList<>(winnersBracket);
        all.addAll(losersBracket);
        all.add(grandFinal);

        return new DoubleElimResult(winnersBracket, losersBracket, grandFinal, all);
    }

    /** Auto-advances any match where exactly one slot is filled (a bye). */
    private static void resolveByes(List<BracketMatch> matches) {
        for (BracketMatch match : matches) {
            if (match.isBye() && match.getStatus() != BracketMatch.Status.COMPLETE) {
                UUID autoWinner = match.getPlayerA() != null ? match.getPlayerA() : match.getPlayerB();
                if (autoWinner != null) {
                    match.declareWinner(autoWinner);
                }
            }
        }
    }

    private static int nextPowerOfTwo(int n) {
        int power = 1;
        while (power < n) power *= 2;
        return Math.max(power, 2);
    }

    public record DoubleElimResult(
            List<BracketMatch> winnersBracket,
            List<BracketMatch> losersBracket,
            BracketMatch grandFinal,
            List<BracketMatch> allMatches
    ) {
    }
}

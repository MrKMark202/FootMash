package hr.fipu.footmash.career;

import java.util.Random;

import hr.fipu.footmash.season.TraitEngine;

/**
 * Pure logic for the season-by-season career simulator. One call to
 * {@link #simulate(PlayerStats, int, Random)} produces a full season summary
 * for a created player: appearances, goals, assists, average rating, the
 * host club's final league position, and the attribute-point reward.
 *
 * <p>All randomness is driven by the caller's {@link Random} — pass a seeded
 * instance from tests. No Android, no DB dependencies.
 */
public final class PlayerCareerEngine {

    public static final int MATCHES_PER_SEASON = 38;
    public static final int MATCHES_PER_HALF   = 19;

    public enum Half { AUTUMN, SPRING }

    private PlayerCareerEngine() {}

    /** Light snapshot of player stats the engine actually consults. */
    public static class PlayerStats {
        public final int overall;
        public final int shooting;
        public final int passing;
        public final int dribbling;
        /** Position string from the seed catalog (e.g. "GK", "ST", "CDM"). */
        public final String position;

        public PlayerStats(int overall, int shooting, int passing, int dribbling,
                           String position) {
            this.overall = overall;
            this.shooting = shooting;
            this.passing = passing;
            this.dribbling = dribbling;
            this.position = position;
        }
    }

    /** Result row returned to the caller; mirrors PlayerCareerSeason fields. */
    public static class SeasonOutcome {
        public final int appearances;
        public final int goals;
        public final int assists;
        public final float avgRating;
        public final int clubFinalPosition;
        public final int pointsEarned;

        public SeasonOutcome(int appearances, int goals, int assists, float avgRating,
                             int clubFinalPosition, int pointsEarned) {
            this.appearances = appearances;
            this.goals = goals;
            this.assists = assists;
            this.avgRating = avgRating;
            this.clubFinalPosition = clubFinalPosition;
            this.pointsEarned = pointsEarned;
        }
    }

    // ─── Public entry points ─────────────────────────────────────────────────

    public static SeasonOutcome simulate(PlayerStats player, int teamOvr, Random rng) {
        int apps              = generateAppearances(player.overall, teamOvr, rng,
            MATCHES_PER_SEASON);
        String group          = TraitEngine.groupOf(player.position);
        int goals             = generateGoals(group, player, apps, rng);
        int assists           = generateAssists(group, player, apps, rng);
        float rating          = generateRating(player.overall, teamOvr, apps, goals, assists, rng);
        int clubFinalPosition = generateClubPosition(teamOvr, rng);
        int pointsEarned      = pointsForRating(rating);

        return new SeasonOutcome(apps, goals, assists, rating, clubFinalPosition, pointsEarned);
    }

    /**
     * Simulates a single half-season (~19 matches). The career hub calls
     * this twice per cycle: once for autumn, once for spring — with a
     * winter transfer window in between. Final position and points are
     * not computed per half; callers consolidate both halves and call
     * {@link #pointsForRating(float)} at season end on the combined
     * average.
     */
    public static SeasonOutcome simulateHalf(PlayerStats player, int teamOvr, Half half,
                                             Random rng) {
        int apps    = generateAppearances(player.overall, teamOvr, rng, MATCHES_PER_HALF);
        String grp  = TraitEngine.groupOf(player.position);
        int goals   = generateGoals(grp, player, apps, rng);
        int assists = generateAssists(grp, player, apps, rng);
        float rating = generateRating(player.overall, teamOvr, apps, goals, assists, rng);
        // Per-half consumers ignore clubFinalPosition and pointsEarned.
        return new SeasonOutcome(apps, goals, assists, rating, 0, 0);
    }

    // ─── Appearances ─────────────────────────────────────────────────────────

    /**
     * Base apps centred at 75% of available matches, shifted by the gap between
     * the player and their team-mates. A player who outclasses the squad locks
     * in near the cap; a weaker player rotates and lands well below.
     *
     * <p>Same shape for full-season and half-season callers — the math scales
     * with {@code matchesAvailable} so the half engine produces ~half-sized
     * appearance totals naturally.
     */
    private static int generateAppearances(int playerOvr, int teamOvr, Random rng,
                                           int matchesAvailable) {
        float base     = matchesAvailable * 0.74f;            // ~28 of 38, ~14 of 19
        float ovrSpan  = matchesAvailable * 0.26f;            // ±10 of 38, ±5 of 19
        float relative = clampF((playerOvr - teamOvr) * 1.5f, -ovrSpan, ovrSpan);
        float noise    = rng.nextFloat() * (matchesAvailable * 0.16f)
                       - (matchesAvailable * 0.08f);          // ±~3 of 38, ±~1.5 of 19
        int apps       = Math.round(base + relative + noise);
        return clampI(apps, 0, matchesAvailable);
    }

    // ─── Goals ───────────────────────────────────────────────────────────────

    private static int generateGoals(String group, PlayerStats p, int apps, Random rng) {
        if (apps == 0) return 0;
        float goalsPerApp = goalRateForGroup(group)
            * ((p.shooting + p.dribbling) / 150f);
        float noise = 0.5f + rng.nextFloat();   // 0.5..1.5
        return Math.max(0, Math.round(goalsPerApp * apps * noise));
    }

    private static float goalRateForGroup(String group) {
        switch (group) {
            case "FW": return 0.40f;
            case "MF": return 0.15f;
            case "DF": return 0.05f;
            default:   return 0.005f; // GK
        }
    }

    // ─── Assists ─────────────────────────────────────────────────────────────

    private static int generateAssists(String group, PlayerStats p, int apps, Random rng) {
        if (apps == 0) return 0;
        float assistsPerApp = assistRateForGroup(group)
            * ((p.passing + p.dribbling) / 150f);
        float noise = 0.5f + rng.nextFloat();
        return Math.max(0, Math.round(assistsPerApp * apps * noise));
    }

    private static float assistRateForGroup(String group) {
        switch (group) {
            case "MF": return 0.25f;
            case "FW": return 0.20f;
            case "DF": return 0.10f;
            default:   return 0.005f; // GK
        }
    }

    // ─── Rating ──────────────────────────────────────────────────────────────

    private static float generateRating(int playerOvr, int teamOvr, int apps,
                                        int goals, int assists, Random rng) {
        if (apps == 0) return 5.5f;
        float productivity = (goals + assists * 0.5f) / apps;
        float rating = 6.5f
            + (playerOvr - teamOvr) * 0.02f
            + productivity * 0.5f
            + rng.nextFloat() * 0.8f - 0.4f; // ±0.4
        return clampF(rating, 5.0f, 9.5f);
    }

    // ─── Club final position ─────────────────────────────────────────────────

    /**
     * Rough placement model: ~76 OVR is a mid-table team in the seeded leagues.
     * Each OVR point above that pulls roughly one position up. Bounded to a
     * 20-team table with ±3 noise so a strong team can still have an off year.
     */
    private static int generateClubPosition(int teamOvr, Random rng) {
        float base = 20f - (teamOvr - 65) / 1.5f;
        float noise = rng.nextFloat() * 6f - 3f;
        return clampI(Math.round(base + noise), 1, 20);
    }

    // ─── Points reward tier ──────────────────────────────────────────────────

    public static int pointsForRating(float avgRating) {
        if (avgRating <  6.5f) return 6;
        if (avgRating <  7.0f) return 10;
        if (avgRating <  7.5f) return 14;
        return 18;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static float clampF(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static int clampI(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}

package hr.fipu.footmash.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import java.util.Map;
import java.util.HashMap;

import hr.fipu.footmash.model.Fixture;
import hr.fipu.footmash.model.RealPlayer;

/**
 * Offline match engine. Designed so that a full season simulated through it
 * produces <em>realistic</em> aggregates:
 *
 * <ul>
 *   <li><b>Outcomes</b> use a Poisson goal model whose expected goals scale
 *       strongly with the effective-rating gap (plus home advantage), so the
 *       strongest sides pull clear — a champion lands around 85-92 points and a
 *       relegated side around 25-32, rather than everyone clustering near 1.8
 *       points per game.</li>
 *   <li><b>Scorers</b> are drawn by a deterministic per-player propensity
 *       (position × finishing quality), and each team has a fixed "talisman"
 *       who takes a large share of its goals. Because the weighting is stable
 *       across the whole season, a genuine striker/winger wins the Golden Boot
 *       with a 20-30 goal tally while centre-backs and keepers almost never
 *       top the chart.</li>
 * </ul>
 */
public class LocalSimulator {

    private static final Random RNG = new Random();
    private static final int DEFAULT_OVR = 78;

    /** Extra effective rating granted to the home side. */
    private static final double HOME_ADVANTAGE = 4.0;
    /** League-average expected goals per team per match. */
    private static final double BASE_XG = 1.35;
    /** How sharply the rating gap swings expected goals. Higher = more separation. */
    private static final double XG_PER_RATING = 0.07;
    /** Probability a goal is scored by the team's designated talisman. */
    private static final double TALISMAN_SHARE = 0.40;

    /**
     * Probability fallback. {@code effectiveRatings} maps each team name to its
     * effective rating (average overall + trait synergy); teams not present fall
     * back to {@link #DEFAULT_OVR}.
     */
    public static List<MatchSimulator.ParsedMatch> simulateAll(
            List<Fixture> fixtures, Map<String, Integer> effectiveRatings,
            Map<String, List<RealPlayer>> rosters) {
        List<MatchSimulator.ParsedMatch> results = new ArrayList<>();
        for (Fixture f : fixtures) {
            int homeAvg = ratingOf(effectiveRatings, f.getHomeTeamName());
            int awayAvg = ratingOf(effectiveRatings, f.getAwayTeamName());
            results.add(simulate(homeAvg, awayAvg,
                f.getHomeTeamName(), f.getAwayTeamName(), rosters));
        }
        return results;
    }

    private static int ratingOf(Map<String, Integer> ratings, String teamName) {
        if (ratings != null && teamName != null) {
            Integer r = ratings.get(teamName);
            if (r != null) return r;
        }
        return DEFAULT_OVR;
    }

    private static MatchSimulator.ParsedMatch simulate(
            int homeAvg, int awayAvg, String homeName, String awayName,
            Map<String, List<RealPlayer>> rosters) {
        double diff = (homeAvg + HOME_ADVANTAGE) - awayAvg;   // home perspective
        double homeXg = clampD(BASE_XG + diff * XG_PER_RATING, 0.15, 4.5);
        double awayXg = clampD(BASE_XG - diff * XG_PER_RATING, 0.15, 4.5);

        int hg = Math.min(6, samplePoisson(homeXg));
        int ag = Math.min(6, samplePoisson(awayXg));

        MatchSimulator.ParsedMatch m = new MatchSimulator.ParsedMatch();
        m.homeGoals = hg;
        m.awayGoals = ag;
        m.scorers   = buildScorers(hg, ag, homeName, awayName, rosters);
        return m;
    }

    /** Knuth's algorithm — a Poisson draw with mean {@code lambda}. */
    private static int samplePoisson(double lambda) {
        double l = Math.exp(-lambda);
        int k = 0;
        double p = 1.0;
        do {
            k++;
            p *= RNG.nextDouble();
        } while (p > l);
        return k - 1;
    }

    // ── Scorers ───────────────────────────────────────────────────────────────

    private static List<MatchSimulator.Scorer> buildScorers(
            int hg, int ag, String homeName, String awayName,
            Map<String, List<RealPlayer>> rosters) {
        List<MatchSimulator.Scorer> list = new ArrayList<>();
        List<Integer> mins = randomMinutes(hg + ag);
        int idx = 0;

        List<RealPlayer> homeRoster = getRosterOrMock(rosters != null ? rosters.get(homeName) : null, homeName);
        List<RealPlayer> awayRoster = getRosterOrMock(rosters != null ? rosters.get(awayName) : null, awayName);

        ScorerPool home = new ScorerPool(homeRoster);
        ScorerPool away = new ScorerPool(awayRoster);

        for (int i = 0; i < hg; i++) {
            MatchSimulator.Scorer s = new MatchSimulator.Scorer();
            RealPlayer scorer = home.pickScorer();
            s.name   = home.displayName(scorer);
            s.team   = "home";
            s.minute = mins.get(idx++);
            s.assist = home.pickAssist(scorer);
            list.add(s);
        }
        for (int i = 0; i < ag; i++) {
            MatchSimulator.Scorer s = new MatchSimulator.Scorer();
            RealPlayer scorer = away.pickScorer();
            s.name   = away.displayName(scorer);
            s.team   = "away";
            s.minute = mins.get(idx++);
            s.assist = away.pickAssist(scorer);
            list.add(s);
        }
        return list;
    }

    /**
     * Per-team scoring model: a fixed talisman plus goal/assist weights derived
     * from each player's position and finishing/creativity. Deterministic for a
     * given roster, so the same strikers accumulate goals all season long.
     */
    private static final class ScorerPool {
        final List<RealPlayer> roster;
        final double[] goalWeight;
        final double[] assistWeight;
        double goalTotal, assistTotal;
        int talisman = -1;

        ScorerPool(List<RealPlayer> roster) {
            this.roster = roster;
            int n = roster == null ? 0 : roster.size();
            goalWeight = new double[n];
            assistWeight = new double[n];
            double best = -1;
            for (int i = 0; i < n; i++) {
                RealPlayer p = roster.get(i);
                goalWeight[i] = scoringWeight(p);
                assistWeight[i] = assistingWeight(p);
                goalTotal += goalWeight[i];
                assistTotal += assistWeight[i];
                if (goalWeight[i] > best) { best = goalWeight[i]; talisman = i; }
            }
        }

        RealPlayer pickScorer() {
            if (roster == null || roster.isEmpty()) return null;
            if (talisman >= 0 && RNG.nextDouble() < TALISMAN_SHARE) return roster.get(talisman);
            int i = pickWeighted(goalWeight, goalTotal);
            return i >= 0 ? roster.get(i) : roster.get(talisman >= 0 ? talisman : 0);
        }

        String pickAssist(RealPlayer scorer) {
            if (roster == null || roster.isEmpty()) return null;
            if (RNG.nextDouble() > 0.55) return null;   // not every goal is assisted
            for (int tries = 0; tries < 5; tries++) {
                int i = pickWeighted(assistWeight, assistTotal);
                if (i < 0) break;
                RealPlayer a = roster.get(i);
                if (a != scorer) return displayName(a);
            }
            return null;
        }

        private int pickWeighted(double[] weights, double total) {
            if (total <= 0) return weights.length > 0 ? RNG.nextInt(weights.length) : -1;
            double r = RNG.nextDouble() * total;
            for (int i = 0; i < weights.length; i++) {
                r -= weights[i];
                if (r <= 0) return i;
            }
            return weights.length - 1;
        }

        /** Initial-and-surname when two squad members share a surname. */
        String displayName(RealPlayer p) {
            if (p == null || p.getName() == null) return "Player";
            String name = p.getName().trim();
            String[] parts = name.split("\\s+");
            if (parts.length < 2) return name;
            String lastName = parts[parts.length - 1];
            int count = 0;
            for (RealPlayer o : roster) {
                if (o.getName() != null && o.getName().endsWith(lastName)) count++;
            }
            return count > 1 ? parts[0].substring(0, 1) + ". " + lastName : name;
        }
    }

    /** Goal propensity: position weight × finishing quality (squared to concentrate). */
    private static double scoringWeight(RealPlayer p) {
        double pos = positionGoalFactor(p.getPosition());
        if (pos <= 0) return 0;
        int ovr = p.getOverall() > 0 ? p.getOverall() : 70;
        int sh = p.getShooting() > 0 ? p.getShooting() : ovr / 2;
        double quality = (ovr / 80.0) * (0.5 + sh / 100.0);
        return pos * quality * quality;
    }

    /** Creativity propensity for assists. */
    private static double assistingWeight(RealPlayer p) {
        double pos = positionAssistFactor(p.getPosition());
        if (pos <= 0) return 0;
        int ovr = p.getOverall() > 0 ? p.getOverall() : 70;
        return pos * (ovr / 80.0);
    }

    private static double positionGoalFactor(String pos) {
        if (pos == null) return 0.20;
        switch (pos.toUpperCase()) {
            case "ST": case "CF": case "SS":          return 1.00;
            case "LW": case "RW": case "LF": case "RF": return 0.62;
            case "CAM": case "AM":                     return 0.42;
            case "CM": case "LM": case "RM":           return 0.22;
            case "CDM": case "DM":                     return 0.10;
            case "RWB": case "LWB":                    return 0.10;
            case "RB": case "LB":                      return 0.07;
            case "CB":                                 return 0.05;
            case "GK":                                 return 0.00;
            default:                                   return 0.20;
        }
    }

    private static double positionAssistFactor(String pos) {
        if (pos == null) return 0.40;
        switch (pos.toUpperCase()) {
            case "CAM": case "AM":                     return 1.00;
            case "LW": case "RW": case "LM": case "RM": return 0.85;
            case "CM":                                 return 0.70;
            case "RWB": case "LWB": case "RB": case "LB": return 0.45;
            case "ST": case "CF": case "SS":           return 0.40;
            case "CDM": case "DM":                     return 0.30;
            case "CB":                                 return 0.15;
            case "GK":                                 return 0.02;
            default:                                   return 0.40;
        }
    }

    // ── Roster fallback (only when a team has no seeded roster) ────────────────

    private static List<RealPlayer> getRosterOrMock(List<RealPlayer> roster, String teamName) {
        if (roster != null && !roster.isEmpty()) return roster;
        List<RealPlayer> mock = new ArrayList<>();
        Random r = new Random(teamName.hashCode());
        String[] line = {"ST", "ST", "LW", "RW", "CAM", "CM", "CM", "CB", "CB", "RB", "GK"};
        for (int i = 0; i < line.length; i++) {
            RealPlayer p = new RealPlayer();
            p.setName(generateMockName(r));
            p.setPosition(line[i]);
            p.setOverall(70 + r.nextInt(10));
            p.setShooting(line[i].equals("ST") ? 70 + r.nextInt(15) : 40 + r.nextInt(30));
            mock.add(p);
        }
        return mock;
    }

    private static String generateMockName(Random r) {
        String[] firstNames = {
            "Luka", "Ivan", "Marko", "Mateo", "David", "Karlo", "Filip", "Josip", "Antonio", "Martin",
            "Leo", "Lucas", "Marc", "Hugo", "Daniel", "Diego", "Alex", "Thomas", "James", "Oliver",
            "Antoine", "Pierre", "Mario", "Alessandro", "Lorenzo", "Gabriel", "Arthur", "Christian"
        };
        String[] lastNames = {
            "Horvat", "Kovačević", "Babić", "Marić", "Novak", "Zubčić", "García", "Rodríguez",
            "González", "Fernández", "López", "Martínez", "Sánchez", "Pérez", "Gomez", "Smith",
            "Jones", "Miller", "Taylor", "Brown", "Wilson", "Müller", "Schmidt", "Fischer",
            "Dubois", "Laurent", "Rossi", "Bianchi", "Silva", "Santos", "Almeida"
        };
        return firstNames[r.nextInt(firstNames.length)] + " " + lastNames[r.nextInt(lastNames.length)];
    }

    private static List<Integer> randomMinutes(int count) {
        List<Integer> mins = new ArrayList<>();
        for (int i = 0; i < count; i++) mins.add(1 + RNG.nextInt(90));
        java.util.Collections.sort(mins);
        return mins;
    }

    private static double clampD(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}

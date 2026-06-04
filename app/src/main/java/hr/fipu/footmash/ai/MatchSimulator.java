package hr.fipu.footmash.ai;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import hr.fipu.footmash.model.Fixture;

public class MatchSimulator {

    /**
     * System-style preamble prepended to every Gemini prompt. Codifies the
     * realism contract for match outcomes and goal scorers so the model
     * weights team strength tiers correctly and never lets a wing-back lead
     * the scoring charts. Kept as one constant so future tuning is a single
     * edit; the per-call prompt then layers the specific fixtures on top.
     */
    public static final String REALISM_PREAMBLE =
        "You are an expert football (soccer) analyst and a dynamic, stateful "
        + "simulation engine designed for multi-season career progression. "
        + "Simulate realistic match outcomes that embed realistic statistical "
        + "distributions and logical variance, while obeying every constraint "
        + "below.\n\n"
        + "### 1. Team Tiering & The Controlled Variance Rule (Chaos Index)\n"
        + "Outcomes must never be completely static, but must stay within "
        + "probabilistic reason.\n"
        + "- Elite Contenders (T1) — e.g. Manchester City, Arsenal, Liverpool, "
        + "Real Madrid, Bayern Munich, PSG. Baseline heavyweights that should "
        + "realistically occupy the top 4. Allow a 10-15% variance where one "
        + "titan has a \"transition year\" and slides to 5th or 6th, opening a "
        + "Champions League spot.\n"
        + "- The \"Smart Play\" Resurgence (T2) — e.g. Manchester United, Chelsea, "
        + "Aston Villa, Juventus, AC Milan. They can win the league or break "
        + "into the top 3, but only when justified by a high-impact tactical "
        + "shift, structural squad harmony, or a lack of European distractions.\n"
        + "- Momentum / Volatility — mid-table and lower sides have variable "
        + "pathing. A stable mid-table side can over-achieve and push for a "
        + "European place.\n"
        + "- No Absolute Shocks — never let a low-tier or newly-promoted team win "
        + "the title or finish top 3 out of nowhere. Keep the champion strictly "
        + "within the realistic top 15% of the league's initial strength.\n\n"
        + "### 2. Player Statistics Realism & Archetype Integrity\n"
        + "- Golden Boot: the top scorer must be a primary attacking asset "
        + "(Striker or elite scoring Winger), with a world-class tally typically "
        + "between 22 and 35 goals in an elite 38-game league.\n"
        + "- Position-Based Caps: defensively-minded midfielders, full-backs and "
        + "wing-backs cannot dominate the scoring charts. A wing-back like "
        + "Jeremie Frimpong cannot be the top scorer with 9 goals. Clamp "
        + "attacking full-backs to roughly 4-8 goals, weighted from overlapping "
        + "runs or set-pieces.\n\n"
        + "### 3. Multi-Season Progression\n"
        + "Treat each result as a seed for the next season: reward sustained "
        + "squad strength and synergy, and let an over-achieving or "
        + "under-performing side carry that trajectory forward rather than "
        + "resetting randomly each matchday.\n\n"
        + "### 4. Output Discipline\n"
        + "Apply all of the above as internal reasoning ONLY. Do NOT output "
        + "tables, prose, narrative, or analysis. Return strictly the JSON "
        + "specified in the instructions that follow.\n\n";

    public static class ParsedMatch {
        @SerializedName("home_goals") public int homeGoals;
        @SerializedName("away_goals") public int awayGoals;
        public List<Scorer> scorers = new ArrayList<>();
    }

    public static class Scorer {
        public String name;
        public String team;
        public int minute;
        /** Optional — name of the team-mate who assisted the goal; may be null. */
        public String assist;
    }

    public static class UserTeamInfo {
        public final String name;
        public final String formation;
        public final int avgOverall;
        public final int avgChemistry;
        public final int synergyDelta;
        public final List<PlayerEntry> players;

        public UserTeamInfo(String name, String formation, int avgOverall,
                            int avgChemistry, int synergyDelta, List<PlayerEntry> players) {
            this.name = name;
            this.formation = formation;
            this.avgOverall = avgOverall;
            this.avgChemistry = avgChemistry;
            this.synergyDelta = synergyDelta;
            this.players = players;
        }

        /** Average overall plus the squad's trait synergy delta. */
        public int effectiveOverall() {
            return avgOverall + synergyDelta;
        }
    }

    public static class PlayerEntry {
        public final String position;
        public final String name;
        public final int overall;
        public final String traits;

        public PlayerEntry(String position, String name, int overall, String traits) {
            this.position = position;
            this.name = name;
            this.overall = overall;
            this.traits = traits;
        }
    }

    /**
     * Builds the full simulation prompt. {@code ratings} maps each team name to its
     * effective rating (average overall + trait synergy) so Gemini can weight every
     * match — not just the user's — by squad strength.
     */
    public static String buildPrompt(List<Fixture> fixtures, UserTeamInfo userTeam,
                                     Map<String, Integer> ratings) {
        StringBuilder sb = new StringBuilder();
        sb.append(REALISM_PREAMBLE);
        sb.append("Simulate ").append(fixtures.size())
          .append(" football matches. Return ONLY a JSON array with exactly ")
          .append(fixtures.size()).append(" results in the same order.\n\n");

        for (int i = 0; i < fixtures.size(); i++) {
            Fixture f = fixtures.get(i);
            sb.append("Match ").append(i + 1).append(": ")
              .append(f.getHomeTeamName()).append(ratingTag(ratings, f.getHomeTeamName()))
              .append(" vs ")
              .append(f.getAwayTeamName()).append(ratingTag(ratings, f.getAwayTeamName()))
              .append("\n");

            if (f.isUserTeam() && userTeam != null) {
                boolean isHome = f.getHomeTeamName().equals(userTeam.name);
                sb.append("  ").append(userTeam.name)
                  .append(" (").append(isHome ? "home" : "away")
                  .append(", ").append(userTeam.formation)
                  .append(", effective OVR ").append(userTeam.effectiveOverall())
                  .append(", chemistry ").append(userTeam.avgChemistry).append("%")
                  .append(", squad synergy ").append(signed(userTeam.synergyDelta))
                  .append(") Starting XI:");
                for (PlayerEntry p : userTeam.players) {
                    sb.append(" ").append(p.name).append("(").append(p.overall);
                    if (p.traits != null && !p.traits.isEmpty()) {
                        sb.append("; ").append(p.traits);
                    }
                    sb.append(")");
                }
                sb.append("\n  Scorers for ").append(userTeam.name)
                  .append(" must use the names listed above; favour attacking traits.\n");
            }
        }

        sb.append("\nReturn ONLY this JSON (no other text):\n");
        sb.append("[{\"home_goals\":1,\"away_goals\":0,\"scorers\":[")
          .append("{\"name\":\"Player\",\"team\":\"home\",\"minute\":67,\"assist\":\"Team-mate\"}]}, ...]\n\n");
        sb.append("Rules: exactly ").append(fixtures.size())
          .append(" objects; scorers.length == home_goals+away_goals; max 4 goals per match; ")
          .append("minutes 1-90; team is \"home\" or \"away\"; optionally add \"assist\" naming the ")
          .append("team-mate who set up the goal (omit it for solo goals); the higher OVR team ")
          .append("should win more often (squad synergy tilts close games) but upsets still happen.\n");

        return sb.toString();
    }

    public static String buildSimplePrompt(List<Fixture> fixtures, Map<String, Integer> ratings) {
        StringBuilder sb = new StringBuilder();
        sb.append(REALISM_PREAMBLE);
        sb.append("Simulate ").append(fixtures.size())
          .append(" football matches. Return ONLY a JSON array.\n");
        for (int i = 0; i < fixtures.size(); i++) {
            Fixture f = fixtures.get(i);
            sb.append(i + 1).append(". ")
              .append(f.getHomeTeamName()).append(ratingTag(ratings, f.getHomeTeamName()))
              .append(" vs ")
              .append(f.getAwayTeamName()).append(ratingTag(ratings, f.getAwayTeamName()))
              .append("\n");
        }
        sb.append("\n[{\"home_goals\":1,\"away_goals\":0,\"scorers\":[{\"name\":\"Player\",\"team\":\"home\",\"minute\":67}]}, ...]\n");
        sb.append("Exactly ").append(fixtures.size())
          .append(" elements. scorers.length==home_goals+away_goals. Max 4 goals. ")
          .append("Higher OVR wins more often. JSON only.\n");
        return sb.toString();
    }

    private static String ratingTag(Map<String, Integer> ratings, String teamName) {
        if (ratings == null) return "";
        Integer r = ratings.get(teamName);
        return r != null ? " (OVR " + r + ")" : "";
    }

    private static String signed(int v) {
        return v > 0 ? "+" + v : String.valueOf(v);
    }

    public static List<ParsedMatch> parseResponse(String raw, int expected) {
        if (raw == null || raw.isEmpty()) return null;
        String json = extractJsonArray(raw);
        if (json == null) return null;
        try {
            Type type = new TypeToken<List<ParsedMatch>>() {}.getType();
            List<ParsedMatch> list = new Gson().fromJson(json, type);
            if (list == null || list.size() != expected) return null;
            for (ParsedMatch m : list) {
                m.homeGoals = Math.max(0, Math.min(7, m.homeGoals));
                m.awayGoals = Math.max(0, Math.min(7, m.awayGoals));
                if (m.scorers == null) m.scorers = new ArrayList<>();
            }
            return list;
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractJsonArray(String text) {
        int start = text.indexOf('[');
        int end   = text.lastIndexOf(']');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return null;
    }
}

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
        "You are an expert football (soccer) analyst and data-driven "
        + "simulation engine. Predict realistic match outcomes that respect "
        + "the constraints below.\n\n"
        + "### 1. Team Tiering & Win Probability\n"
        + "- Elite Contenders (Manchester City, Arsenal, Liverpool, Real Madrid, "
        + "Bayern Munich, PSG, etc.) form the core of the title race and should "
        + "consistently finish top based on real-world sporting and financial "
        + "dominance.\n"
        + "- The \"Smart Play\" rule: historically strong but fluctuating teams "
        + "(Manchester United, Chelsea, Juventus, AC Milan) can still finish "
        + "top 3 or win the league under realistic conditions (tactical mastery, "
        + "squad cohesion, fewer injuries).\n"
        + "- No severe shocks: do not let newly-promoted or mid-table sides "
        + "behave like Leicester City 2016. Keep the champion within the "
        + "realistic top 15% of the league's strongest teams.\n\n"
        + "### 2. Player Statistics Realism\n"
        + "- Top scorers must be dedicated attacking players (Strikers, Wingers).\n"
        + "- A top scorer in an elite league has a realistic tally between 22 "
        + "and 35 goals depending on dominance (Haaland-style).\n"
        + "- Defensively-minded players, wing-backs, and traditional defensive "
        + "midfielders cannot lead the scoring charts. A wing-back like Frimpong "
        + "cannot win the Golden Boot with only 9 goals. If a defender scores, "
        + "keep it realistic: 3 to 7 goals max from set pieces and overlapping "
        + "runs.\n\n";

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

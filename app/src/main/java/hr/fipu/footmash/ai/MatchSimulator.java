package hr.fipu.footmash.ai;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.model.Fixture;

public class MatchSimulator {

    public static class ParsedMatch {
        @SerializedName("home_goals") public int homeGoals;
        @SerializedName("away_goals") public int awayGoals;
        public List<Scorer> scorers = new ArrayList<>();
    }

    public static class Scorer {
        public String name;
        public String team;
        public int minute;
    }

    public static class UserTeamInfo {
        public final String name;
        public final String formation;
        public final int avgOverall;
        public final int avgChemistry;
        public final List<PlayerEntry> players;

        public UserTeamInfo(String name, String formation, int avgOverall,
                            int avgChemistry, List<PlayerEntry> players) {
            this.name = name;
            this.formation = formation;
            this.avgOverall = avgOverall;
            this.avgChemistry = avgChemistry;
            this.players = players;
        }
    }

    public static class PlayerEntry {
        public final String position;
        public final String name;
        public final int overall;

        public PlayerEntry(String position, String name, int overall) {
            this.position = position;
            this.name = name;
            this.overall = overall;
        }
    }

    public static String buildPrompt(List<Fixture> fixtures, UserTeamInfo userTeam) {
        StringBuilder sb = new StringBuilder();
        sb.append("Simulate ").append(fixtures.size())
          .append(" football matches. Return ONLY a JSON array with exactly ")
          .append(fixtures.size()).append(" results in the same order.\n\n");

        for (int i = 0; i < fixtures.size(); i++) {
            Fixture f = fixtures.get(i);
            sb.append("Match ").append(i + 1).append(": ")
              .append(f.getHomeTeamName()).append(" vs ").append(f.getAwayTeamName()).append("\n");

            if (f.isUserTeam() && userTeam != null) {
                boolean isHome = f.getHomeTeamName().equals(userTeam.name);
                sb.append("  ").append(userTeam.name)
                  .append(" (").append(isHome ? "home" : "away")
                  .append(", ").append(userTeam.formation)
                  .append(", avg OVR ").append(userTeam.avgOverall)
                  .append(", chemistry ").append(userTeam.avgChemistry).append("%")
                  .append(") Starting XI:");
                for (PlayerEntry p : userTeam.players) {
                    sb.append(" ").append(p.name).append("(").append(p.overall).append(")");
                }
                sb.append("\n  Scorers for ").append(userTeam.name)
                  .append(" must use the names listed above.\n");
            }
        }

        sb.append("\nReturn ONLY this JSON (no other text):\n");
        sb.append("[{\"home_goals\":1,\"away_goals\":0,\"scorers\":[{\"name\":\"Player\",\"team\":\"home\",\"minute\":67}]}, ...]\n\n");
        sb.append("Rules: exactly ").append(fixtures.size())
          .append(" objects; scorers.length == home_goals+away_goals; max 4 goals per match; ")
          .append("minutes 1-90; team is \"home\" or \"away\"; realistic scorelines.\n");

        return sb.toString();
    }

    public static String buildSimplePrompt(List<Fixture> fixtures) {
        StringBuilder sb = new StringBuilder();
        sb.append("Simulate ").append(fixtures.size())
          .append(" football matches. Return ONLY a JSON array.\n");
        for (int i = 0; i < fixtures.size(); i++) {
            Fixture f = fixtures.get(i);
            sb.append(i + 1).append(". ")
              .append(f.getHomeTeamName()).append(" vs ").append(f.getAwayTeamName()).append("\n");
        }
        sb.append("\n[{\"home_goals\":1,\"away_goals\":0,\"scorers\":[{\"name\":\"Player\",\"team\":\"home\",\"minute\":67}]}, ...]\n");
        sb.append("Exactly ").append(fixtures.size())
          .append(" elements. scorers.length==home_goals+away_goals. Max 4 goals. JSON only.\n");
        return sb.toString();
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

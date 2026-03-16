package hr.fipu.footmash.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Model za Standing odgovor iz /standings endpoint-a.
 * Struktura: { "league": { "id": ..., "standings": [[...]] } }
 */
public class StandingResponse {

    @SerializedName("league")
    private LeagueStandings league;

    public LeagueStandings getLeague() { return league; }

    public static class LeagueStandings {
        @SerializedName("id")
        private int id;
        @SerializedName("name")
        private String name;
        @SerializedName("country")
        private String country;
        @SerializedName("logo")
        private String logo;
        @SerializedName("flag")
        private String flag;
        @SerializedName("season")
        private int season;
        @SerializedName("standings")
        private List<List<StandingEntry>> standings;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getCountry() { return country; }
        public String getLogo() { return logo; }
        public String getFlag() { return flag; }
        public int getSeason() { return season; }
        public List<List<StandingEntry>> getStandings() { return standings; }
    }

    public static class StandingEntry {
        @SerializedName("rank")
        private int rank;
        @SerializedName("team")
        private StandingTeam team;
        @SerializedName("points")
        private int points;
        @SerializedName("goalsDiff")
        private int goalsDiff;
        @SerializedName("group")
        private String group;
        @SerializedName("form")
        private String form;
        @SerializedName("status")
        private String status;
        @SerializedName("description")
        private String description;
        @SerializedName("all")
        private MatchStats all;
        @SerializedName("home")
        private MatchStats home;
        @SerializedName("away")
        private MatchStats away;

        public int getRank() { return rank; }
        public StandingTeam getTeam() { return team; }
        public int getPoints() { return points; }
        public int getGoalsDiff() { return goalsDiff; }
        public String getGroup() { return group; }
        public String getForm() { return form; }
        public String getStatus() { return status; }
        public String getDescription() { return description; }
        public MatchStats getAll() { return all; }
        public MatchStats getHome() { return home; }
        public MatchStats getAway() { return away; }
    }

    public static class StandingTeam {
        @SerializedName("id")
        private int id;
        @SerializedName("name")
        private String name;
        @SerializedName("logo")
        private String logo;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getLogo() { return logo; }
    }

    public static class MatchStats {
        @SerializedName("played")
        private int played;
        @SerializedName("win")
        private int win;
        @SerializedName("draw")
        private int draw;
        @SerializedName("lose")
        private int lose;
        @SerializedName("goals")
        private GoalStats goals;

        public int getPlayed() { return played; }
        public int getWin() { return win; }
        public int getDraw() { return draw; }
        public int getLose() { return lose; }
        public GoalStats getGoals() { return goals; }
    }

    public static class GoalStats {
        @SerializedName("for")
        private int goalsFor;
        @SerializedName("against")
        private int goalsAgainst;

        public int getGoalsFor() { return goalsFor; }
        public int getGoalsAgainst() { return goalsAgainst; }
    }
}

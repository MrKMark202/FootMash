package hr.fipu.footmash.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model za Team Statistics odgovor iz /teams/statistics endpoint-a.
 * Ovo je poseban format – nema wrappera ApiResponse<T>.
 */
public class TeamStatisticsResponse {

    @SerializedName("get")
    private String get;

    @SerializedName("results")
    private int results;

    @SerializedName("response")
    private TeamStats response;

    public TeamStats getResponse() { return response; }

    public static class TeamStats {
        @SerializedName("league")
        private StatLeague league;
        @SerializedName("team")
        private StatTeam team;
        @SerializedName("form")
        private String form;
        @SerializedName("fixtures")
        private Fixtures fixtures;
        @SerializedName("goals")
        private GoalsDetail goals;
        @SerializedName("biggest")
        private Biggest biggest;
        @SerializedName("clean_sheet")
        private HomeAwayTotal cleanSheet;
        @SerializedName("failed_to_score")
        private HomeAwayTotal failedToScore;
        @SerializedName("penalty")
        private PenaltyStats penalty;
        @SerializedName("lineups")
        private Object lineups;
        @SerializedName("cards")
        private Object cards;

        public StatLeague getLeague() { return league; }
        public StatTeam getTeam() { return team; }
        public String getForm() { return form; }
        public Fixtures getFixtures() { return fixtures; }
        public GoalsDetail getGoals() { return goals; }
        public Biggest getBiggest() { return biggest; }
        public HomeAwayTotal getCleanSheet() { return cleanSheet; }
        public HomeAwayTotal getFailedToScore() { return failedToScore; }
        public PenaltyStats getPenalty() { return penalty; }
    }

    public static class StatLeague {
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

    public static class StatTeam {
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

    public static class Fixtures {
        @SerializedName("played")
        private HomeAwayTotal played;
        @SerializedName("wins")
        private HomeAwayTotal wins;
        @SerializedName("draws")
        private HomeAwayTotal draws;
        @SerializedName("loses")
        private HomeAwayTotal loses;

        public HomeAwayTotal getPlayed() { return played; }
        public HomeAwayTotal getWins() { return wins; }
        public HomeAwayTotal getDraws() { return draws; }
        public HomeAwayTotal getLoses() { return loses; }
    }

    public static class HomeAwayTotal {
        @SerializedName("home")
        private int home;
        @SerializedName("away")
        private int away;
        @SerializedName("total")
        private int total;

        public int getHome() { return home; }
        public int getAway() { return away; }
        public int getTotal() { return total; }
    }

    public static class GoalsDetail {
        @SerializedName("for")
        private GoalDirection goalsFor;
        @SerializedName("against")
        private GoalDirection goalsAgainst;

        public GoalDirection getGoalsFor() { return goalsFor; }
        public GoalDirection getGoalsAgainst() { return goalsAgainst; }
    }

    public static class GoalDirection {
        @SerializedName("total")
        private HomeAwayTotal total;
        @SerializedName("average")
        private HomeAwayAvg average;

        public HomeAwayTotal getTotal() { return total; }
        public HomeAwayAvg getAverage() { return average; }
    }

    public static class HomeAwayAvg {
        @SerializedName("home")
        private String home;
        @SerializedName("away")
        private String away;
        @SerializedName("total")
        private String total;

        public String getHome() { return home; }
        public String getAway() { return away; }
        public String getTotal() { return total; }
    }

    public static class Biggest {
        @SerializedName("streak")
        private Streak streak;
        @SerializedName("wins")
        private HomeAway wins;
        @SerializedName("loses")
        private HomeAway loses;
        @SerializedName("goals")
        private BiggestGoals goals;

        public Streak getStreak() { return streak; }
        public HomeAway getWins() { return wins; }
        public HomeAway getLoses() { return loses; }
        public BiggestGoals getGoals() { return goals; }
    }

    public static class Streak {
        @SerializedName("wins")
        private int wins;
        @SerializedName("draws")
        private int draws;
        @SerializedName("loses")
        private int loses;

        public int getWins() { return wins; }
        public int getDraws() { return draws; }
        public int getLoses() { return loses; }
    }

    public static class HomeAway {
        @SerializedName("home")
        private String home;
        @SerializedName("away")
        private String away;

        public String getHome() { return home; }
        public String getAway() { return away; }
    }

    public static class BiggestGoals {
        @SerializedName("for")
        private HomeAwayInt goalsFor;
        @SerializedName("against")
        private HomeAwayInt goalsAgainst;

        public HomeAwayInt getGoalsFor() { return goalsFor; }
        public HomeAwayInt getGoalsAgainst() { return goalsAgainst; }
    }

    public static class HomeAwayInt {
        @SerializedName("home")
        private int home;
        @SerializedName("away")
        private int away;

        public int getHome() { return home; }
        public int getAway() { return away; }
    }

    public static class PenaltyStats {
        @SerializedName("scored")
        private PenaltyDetail scored;
        @SerializedName("missed")
        private PenaltyDetail missed;
        @SerializedName("total")
        private int total;

        public PenaltyDetail getScored() { return scored; }
        public PenaltyDetail getMissed() { return missed; }
        public int getTotal() { return total; }
    }

    public static class PenaltyDetail {
        @SerializedName("total")
        private int total;
        @SerializedName("percentage")
        private String percentage;

        public int getTotal() { return total; }
        public String getPercentage() { return percentage; }
    }
}

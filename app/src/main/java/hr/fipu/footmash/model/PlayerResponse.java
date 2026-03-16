package hr.fipu.footmash.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Model za Player odgovor iz /players endpoint-a.
 * Struktura: { "player": {...}, "statistics": [...] }
 */
public class PlayerResponse {

    @SerializedName("player")
    private PlayerInfo player;

    @SerializedName("statistics")
    private List<Statistics> statistics;

    public PlayerInfo getPlayer() { return player; }
    public List<Statistics> getStatistics() { return statistics; }

    public static class PlayerInfo {
        @SerializedName("id")
        private int id;
        @SerializedName("name")
        private String name;
        @SerializedName("firstname")
        private String firstname;
        @SerializedName("lastname")
        private String lastname;
        @SerializedName("age")
        private int age;
        @SerializedName("nationality")
        private String nationality;
        @SerializedName("height")
        private String height;
        @SerializedName("weight")
        private String weight;
        @SerializedName("injured")
        private boolean injured;
        @SerializedName("photo")
        private String photo;
        @SerializedName("birth")
        private Birth birth;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getFirstname() { return firstname; }
        public String getLastname() { return lastname; }
        public int getAge() { return age; }
        public String getNationality() { return nationality; }
        public String getHeight() { return height; }
        public String getWeight() { return weight; }
        public boolean isInjured() { return injured; }
        public String getPhoto() { return photo; }
        public Birth getBirth() { return birth; }
    }

    public static class Birth {
        @SerializedName("date")
        private String date;
        @SerializedName("place")
        private String place;
        @SerializedName("country")
        private String country;

        public String getDate() { return date; }
        public String getPlace() { return place; }
        public String getCountry() { return country; }
    }

    public static class Statistics {
        @SerializedName("team")
        private StatTeam team;
        @SerializedName("league")
        private StatLeague league;
        @SerializedName("games")
        private Games games;
        @SerializedName("goals")
        private Goals goals;
        @SerializedName("passes")
        private Passes passes;
        @SerializedName("tackles")
        private Tackles tackles;
        @SerializedName("duels")
        private Duels duels;
        @SerializedName("dribbles")
        private Dribbles dribbles;
        @SerializedName("fouls")
        private Fouls fouls;
        @SerializedName("cards")
        private Cards cards;
        @SerializedName("penalty")
        private Penalty penalty;
        @SerializedName("shots")
        private Shots shots;

        public StatTeam getTeam() { return team; }
        public StatLeague getLeague() { return league; }
        public Games getGames() { return games; }
        public Goals getGoals() { return goals; }
        public Passes getPasses() { return passes; }
        public Tackles getTackles() { return tackles; }
        public Duels getDuels() { return duels; }
        public Dribbles getDribbles() { return dribbles; }
        public Fouls getFouls() { return fouls; }
        public Cards getCards() { return cards; }
        public Penalty getPenalty() { return penalty; }
        public Shots getShots() { return shots; }
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

    public static class StatLeague {
        @SerializedName("id")
        private int id;
        @SerializedName("name")
        private String name;
        @SerializedName("country")
        private String country;
        @SerializedName("logo")
        private String logo;
        @SerializedName("season")
        private int season;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getCountry() { return country; }
        public String getLogo() { return logo; }
        public int getSeason() { return season; }
    }

    public static class Games {
        @SerializedName("appearences")
        private Integer appearences;
        @SerializedName("lineups")
        private Integer lineups;
        @SerializedName("minutes")
        private Integer minutes;
        @SerializedName("position")
        private String position;
        @SerializedName("rating")
        private String rating;
        @SerializedName("captain")
        private boolean captain;

        public Integer getAppearences() { return appearences; }
        public Integer getLineups() { return lineups; }
        public Integer getMinutes() { return minutes; }
        public String getPosition() { return position; }
        public String getRating() { return rating; }
        public boolean isCaptain() { return captain; }
    }

    public static class Goals {
        @SerializedName("total")
        private Integer total;
        @SerializedName("conceded")
        private Integer conceded;
        @SerializedName("assists")
        private Integer assists;
        @SerializedName("saves")
        private Integer saves;

        public Integer getTotal() { return total; }
        public Integer getConceded() { return conceded; }
        public Integer getAssists() { return assists; }
        public Integer getSaves() { return saves; }
    }

    public static class Passes {
        @SerializedName("total")
        private Integer total;
        @SerializedName("key")
        private Integer key;
        @SerializedName("accuracy")
        private Integer accuracy;

        public Integer getTotal() { return total; }
        public Integer getKey() { return key; }
        public Integer getAccuracy() { return accuracy; }
    }

    public static class Tackles {
        @SerializedName("total")
        private Integer total;
        @SerializedName("blocks")
        private Integer blocks;
        @SerializedName("interceptions")
        private Integer interceptions;

        public Integer getTotal() { return total; }
        public Integer getBlocks() { return blocks; }
        public Integer getInterceptions() { return interceptions; }
    }

    public static class Duels {
        @SerializedName("total")
        private Integer total;
        @SerializedName("won")
        private Integer won;

        public Integer getTotal() { return total; }
        public Integer getWon() { return won; }
    }

    public static class Dribbles {
        @SerializedName("attempts")
        private Integer attempts;
        @SerializedName("success")
        private Integer success;
        @SerializedName("past")
        private Integer past;

        public Integer getAttempts() { return attempts; }
        public Integer getSuccess() { return success; }
        public Integer getPast() { return past; }
    }

    public static class Fouls {
        @SerializedName("drawn")
        private Integer drawn;
        @SerializedName("committed")
        private Integer committed;

        public Integer getDrawn() { return drawn; }
        public Integer getCommitted() { return committed; }
    }

    public static class Cards {
        @SerializedName("yellow")
        private Integer yellow;
        @SerializedName("yellowred")
        private Integer yellowred;
        @SerializedName("red")
        private Integer red;

        public Integer getYellow() { return yellow; }
        public Integer getYellowred() { return yellowred; }
        public Integer getRed() { return red; }
    }

    public static class Penalty {
        @SerializedName("won")
        private Integer won;
        @SerializedName("commited")
        private Integer commited;
        @SerializedName("scored")
        private Integer scored;
        @SerializedName("missed")
        private Integer missed;
        @SerializedName("saved")
        private Integer saved;

        public Integer getWon() { return won; }
        public Integer getCommited() { return commited; }
        public Integer getScored() { return scored; }
        public Integer getMissed() { return missed; }
        public Integer getSaved() { return saved; }
    }

    public static class Shots {
        @SerializedName("total")
        private Integer total;
        @SerializedName("on")
        private Integer on;

        public Integer getTotal() { return total; }
        public Integer getOn() { return on; }
    }
}

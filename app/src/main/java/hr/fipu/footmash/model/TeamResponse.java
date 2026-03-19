package hr.fipu.footmash.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model za Team odgovor iz /teams endpoint-a.
 * Struktura: { "team": {...}, "venue": {...} }
 */
public class TeamResponse {

    @SerializedName("team_key")
    private int teamKey;

    @SerializedName("team_name")
    private String teamName;

    @SerializedName("team_badge")
    private String teamBadge;

    @SerializedName("players")
    private java.util.List<PlayerInfo> players;

    public int getTeamKey() { return teamKey; }
    public String getTeamName() { return teamName; }
    public String getTeamBadge() { return teamBadge; }
    public java.util.List<PlayerInfo> getPlayers() { return players; }

    public static class PlayerInfo {
        @SerializedName("player_key")
        private long playerKey;

        @SerializedName("player_name")
        private String playerName;

        @SerializedName("player_number")
        private String playerNumber;

        @SerializedName("player_country")
        private String playerCountry;

        @SerializedName("player_type")
        private String playerType;

        @SerializedName("player_age")
        private String playerAge;

        @SerializedName("player_image")
        private String playerImage;

        public long getPlayerKey() { return playerKey; }
        public String getPlayerName() { return playerName; }
        public String getPlayerNumber() { return playerNumber; }
        public String getPlayerCountry() { return playerCountry; }
        public String getPlayerType() { return playerType; }
        public String getPlayerAge() { return playerAge; }
        public String getPlayerImage() { return playerImage; }
    }
}

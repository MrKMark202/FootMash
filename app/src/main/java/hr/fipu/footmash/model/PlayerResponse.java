package hr.fipu.footmash.model;

import com.google.gson.annotations.SerializedName;

/**
 * Plain POJO for a player. Populated from local seed data.
 */
public class PlayerResponse {
    @SerializedName("player_key")
    private int playerKey;

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

    @SerializedName("player_match_played")
    private String playerMatchPlayed;

    @SerializedName("player_goals")
    private String playerGoals;

    @SerializedName("player_yellow_cards")
    private String playerYellowCards;

    @SerializedName("player_red_cards")
    private String playerRedCards;

    @SerializedName("player_image")
    private String playerImage;

    public int getPlayerKey() { return playerKey; }
    public String getPlayerName() { return playerName; }
    public String getPlayerNumber() { return playerNumber; }
    public String getPlayerCountry() { return playerCountry; }
    public String getPlayerType() { return playerType; }
    public String getPlayerAge() { return playerAge; }
    public String getPlayerMatchPlayed() { return playerMatchPlayed; }
    public String getPlayerGoals() { return playerGoals; }
    public String getPlayerYellowCards() { return playerYellowCards; }
    public String getPlayerRedCards() { return playerRedCards; }
    public String getPlayerImage() { return playerImage; }

    public void setPlayerKey(int playerKey) { this.playerKey = playerKey; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public void setPlayerNumber(String playerNumber) { this.playerNumber = playerNumber; }
    public void setPlayerCountry(String playerCountry) { this.playerCountry = playerCountry; }
    public void setPlayerType(String playerType) { this.playerType = playerType; }
    public void setPlayerAge(String playerAge) { this.playerAge = playerAge; }
    public void setPlayerMatchPlayed(String playerMatchPlayed) { this.playerMatchPlayed = playerMatchPlayed; }
    public void setPlayerGoals(String playerGoals) { this.playerGoals = playerGoals; }
    public void setPlayerYellowCards(String playerYellowCards) { this.playerYellowCards = playerYellowCards; }
    public void setPlayerRedCards(String playerRedCards) { this.playerRedCards = playerRedCards; }
    public void setPlayerImage(String playerImage) { this.playerImage = playerImage; }
}

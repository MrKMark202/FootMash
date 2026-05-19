package hr.fipu.footmash.model;

import com.google.gson.annotations.SerializedName;

/**
 * Plain POJO for a league. Populated from local seed data.
 */
public class LeagueResponse {
    @SerializedName("league_key")
    private int leagueKey;

    @SerializedName("league_name")
    private String leagueName;

    @SerializedName("country_key")
    private int countryKey;

    @SerializedName("country_name")
    private String countryName;

    @SerializedName("league_logo")
    private String leagueLogo;

    @SerializedName("country_logo")
    private String countryLogo;

    public int getLeagueKey() { return leagueKey; }
    public String getLeagueName() { return leagueName; }
    public int getCountryKey() { return countryKey; }
    public String getCountryName() { return countryName; }
    public String getLeagueLogo() { return leagueLogo; }
    public String getCountryLogo() { return countryLogo; }

    public void setLeagueKey(int leagueKey) { this.leagueKey = leagueKey; }
    public void setLeagueName(String leagueName) { this.leagueName = leagueName; }
    public void setCountryKey(int countryKey) { this.countryKey = countryKey; }
    public void setCountryName(String countryName) { this.countryName = countryName; }
    public void setLeagueLogo(String leagueLogo) { this.leagueLogo = leagueLogo; }
    public void setCountryLogo(String countryLogo) { this.countryLogo = countryLogo; }
}

package hr.fipu.footmash.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Model za League odgovor iz /leagues endpoint-a.
 * Struktura: { "league": {...}, "country": {...}, "seasons": [...] }
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
}

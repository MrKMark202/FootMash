package hr.fipu.footmash.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Model za League odgovor iz /leagues endpoint-a.
 * Struktura: { "league": {...}, "country": {...}, "seasons": [...] }
 */
public class LeagueResponse {

    @SerializedName("league")
    private LeagueInfo league;

    @SerializedName("country")
    private CountryInfo country;

    @SerializedName("seasons")
    private List<Season> seasons;

    public LeagueInfo getLeague() { return league; }
    public CountryInfo getCountry() { return country; }
    public List<Season> getSeasons() { return seasons; }

    public static class LeagueInfo {
        @SerializedName("id")
        private int id;
        @SerializedName("name")
        private String name;
        @SerializedName("type")
        private String type;
        @SerializedName("logo")
        private String logo;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getType() { return type; }
        public String getLogo() { return logo; }
    }

    public static class CountryInfo {
        @SerializedName("name")
        private String name;
        @SerializedName("code")
        private String code;
        @SerializedName("flag")
        private String flag;

        public String getName() { return name; }
        public String getCode() { return code; }
        public String getFlag() { return flag; }
    }

    public static class Season {
        @SerializedName("year")
        private int year;
        @SerializedName("start")
        private String start;
        @SerializedName("end")
        private String end;
        @SerializedName("current")
        private boolean current;

        public int getYear() { return year; }
        public String getStart() { return start; }
        public String getEnd() { return end; }
        public boolean isCurrent() { return current; }
    }
}

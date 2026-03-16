package hr.fipu.footmash.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model za Team odgovor iz /teams endpoint-a.
 * Struktura: { "team": {...}, "venue": {...} }
 */
public class TeamResponse {

    @SerializedName("team")
    private TeamInfo team;

    @SerializedName("venue")
    private VenueInfo venue;

    public TeamInfo getTeam() { return team; }
    public VenueInfo getVenue() { return venue; }

    public static class TeamInfo {
        @SerializedName("id")
        private int id;
        @SerializedName("name")
        private String name;
        @SerializedName("code")
        private String code;
        @SerializedName("country")
        private String country;
        @SerializedName("founded")
        private int founded;
        @SerializedName("national")
        private boolean national;
        @SerializedName("logo")
        private String logo;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getCode() { return code; }
        public String getCountry() { return country; }
        public int getFounded() { return founded; }
        public boolean isNational() { return national; }
        public String getLogo() { return logo; }
    }

    public static class VenueInfo {
        @SerializedName("id")
        private int id;
        @SerializedName("name")
        private String name;
        @SerializedName("address")
        private String address;
        @SerializedName("city")
        private String city;
        @SerializedName("capacity")
        private int capacity;
        @SerializedName("surface")
        private String surface;
        @SerializedName("image")
        private String image;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getAddress() { return address; }
        public String getCity() { return city; }
        public int getCapacity() { return capacity; }
        public String getSurface() { return surface; }
        public String getImage() { return image; }
    }
}

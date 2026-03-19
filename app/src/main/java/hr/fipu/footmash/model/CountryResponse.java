package hr.fipu.footmash.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model za Country odgovor iz AllSportsAPI /football/?met=Countries endpoint-a.
 */
public class CountryResponse {

    @SerializedName("country_key")
    private int countryKey;

    @SerializedName("country_name")
    private String countryName;

    @SerializedName("country_logo")
    private String countryLogo;

    public int getCountryKey() { return countryKey; }
    public String getCountryName() { return countryName; }
    public String getCountryLogo() { return countryLogo; }

    public void setCountryKey(int countryKey) { this.countryKey = countryKey; }
    public void setCountryName(String countryName) { this.countryName = countryName; }
    public void setCountryLogo(String countryLogo) { this.countryLogo = countryLogo; }
}

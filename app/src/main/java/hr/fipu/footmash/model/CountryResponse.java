package hr.fipu.footmash.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model za Country odgovor iz /countries endpoint-a.
 * Vraća direktno: { "name": "...", "code": "...", "flag": "..." }
 */
public class CountryResponse {

    @SerializedName("name")
    private String name;

    @SerializedName("code")
    private String code;

    @SerializedName("flag")
    private String flag;

    public String getName() { return name; }
    public String getCode() { return code; }
    public String getFlag() { return flag; }

    public void setName(String name) { this.name = name; }
    public void setCode(String code) { this.code = code; }
    public void setFlag(String flag) { this.flag = flag; }
}

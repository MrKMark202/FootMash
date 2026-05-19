package hr.fipu.footmash.model;

import androidx.room.ColumnInfo;

public class LeagueInfo {

    @ColumnInfo(name = "leagueId")
    private int id;

    @ColumnInfo(name = "leagueName")
    private String name;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

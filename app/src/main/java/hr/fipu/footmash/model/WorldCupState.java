package hr.fipu.footmash.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Single-row holder for the active World Cup playthrough. The whole tournament
 * (squad, groups, fixtures, bracket) is a bracket-shaped graph, so it is stored
 * as one Gson JSON blob rather than spread across relational tables. {@code id}
 * is always {@link #ROW_ID}; a missing row means "no tournament in progress".
 */
@Entity(tableName = "world_cup")
public class WorldCupState {

    public static final int ROW_ID = 1;

    @PrimaryKey
    private int id = ROW_ID;

    private String json;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getJson() { return json; }
    public void setJson(String json) { this.json = json; }
}

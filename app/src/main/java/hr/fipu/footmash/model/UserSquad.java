package hr.fipu.footmash.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_squad")
public class UserSquad {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int clubId;
    private int playerId;
    private boolean isStartingXI;
    private String pitchPosition;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getClubId() { return clubId; }
    public void setClubId(int clubId) { this.clubId = clubId; }

    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }

    public boolean isStartingXI() { return isStartingXI; }
    public void setStartingXI(boolean startingXI) { isStartingXI = startingXI; }

    public String getPitchPosition() { return pitchPosition; }
    public void setPitchPosition(String pitchPosition) { this.pitchPosition = pitchPosition; }
}

package hr.fipu.footmash.worldcup;

import hr.fipu.footmash.model.RealPlayer;

/**
 * A lightweight squad player for World Cup mode. Wraps either a real player from
 * the bundled datasets ({@code id} = {@code real_players.id}) or a generated
 * filler used to top up a thin national pool ({@code id} negative,
 * {@code filler == true}). Serialized into the tournament JSON blob via Gson, so
 * fields are public and the no-arg constructor is kept.
 */
public class WcPlayer {

    public int id;
    public String name;
    public String position;  // GK, RB, CB, LB, CM, CDM, CAM, ST, RW, LW …
    public int overall;
    public boolean filler;

    public WcPlayer() {}

    public WcPlayer(int id, String name, String position, int overall, boolean filler) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.overall = overall;
        this.filler = filler;
    }

    public static WcPlayer fromReal(RealPlayer p) {
        return new WcPlayer(p.getId(), p.getName(), p.getPosition(),
            p.getEffectiveOverall(), false);
    }

    /**
     * A {@link RealPlayer} view for the rating/simulation engine. Filler players
     * have no detailed attributes, so the six FIFA stats are seeded from the
     * overall — enough for {@code TraitEngine}/{@code LocalSimulator}.
     */
    public RealPlayer toReal() {
        RealPlayer p = new RealPlayer();
        p.setId(id);
        p.setName(name);
        p.setPosition(position);
        p.setOverall(overall);
        p.setPace(overall);
        p.setShooting(overall);
        p.setPassing(overall);
        p.setDribbling(overall);
        p.setDefending(overall);
        p.setPhysical(overall);
        return p;
    }
}

package hr.fipu.footmash.model;

public class FormationSlot {
    public final String key;
    public final String label;
    public final String posGroup;
    public final float xPct;
    public final float yPct;

    public FormationSlot(String key, String label, String posGroup, float xPct, float yPct) {
        this.key = key;
        this.label = label;
        this.posGroup = posGroup;
        this.xPct = xPct;
        this.yPct = yPct;
    }
}

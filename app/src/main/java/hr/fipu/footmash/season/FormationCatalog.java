package hr.fipu.footmash.season;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hr.fipu.footmash.model.FormationSlot;

public final class FormationCatalog {

    public static final Map<String, List<FormationSlot>> FORMATIONS = new LinkedHashMap<>();

    public static final String DEFAULT = "4-3-3";

    static {
        FORMATIONS.put("4-4-2", Arrays.asList(
            new FormationSlot("GK",   "GK",  "GK",  0.50f, 0.85f),
            new FormationSlot("LB",   "LB",  "DF",  0.10f, 0.72f),
            new FormationSlot("CB1",  "CB",  "DF",  0.33f, 0.72f),
            new FormationSlot("CB2",  "CB",  "DF",  0.67f, 0.72f),
            new FormationSlot("RB",   "RB",  "DF",  0.90f, 0.72f),
            new FormationSlot("LM",   "LM",  "MF",  0.10f, 0.50f),
            new FormationSlot("CM1",  "CM",  "MF",  0.33f, 0.50f),
            new FormationSlot("CM2",  "CM",  "MF",  0.67f, 0.50f),
            new FormationSlot("RM",   "RM",  "MF",  0.90f, 0.50f),
            new FormationSlot("ST1",  "ST",  "FW",  0.33f, 0.20f),
            new FormationSlot("ST2",  "ST",  "FW",  0.67f, 0.20f)
        ));
        FORMATIONS.put("4-3-3", Arrays.asList(
            new FormationSlot("GK",   "GK",  "GK",  0.50f, 0.85f),
            new FormationSlot("LB",   "LB",  "DF",  0.10f, 0.72f),
            new FormationSlot("CB1",  "CB",  "DF",  0.33f, 0.72f),
            new FormationSlot("CB2",  "CB",  "DF",  0.67f, 0.72f),
            new FormationSlot("RB",   "RB",  "DF",  0.90f, 0.72f),
            new FormationSlot("CM1",  "CM",  "MF",  0.20f, 0.50f),
            new FormationSlot("CM2",  "CM",  "MF",  0.50f, 0.50f),
            new FormationSlot("CM3",  "CM",  "MF",  0.80f, 0.50f),
            new FormationSlot("LW",   "LW",  "FW",  0.15f, 0.20f),
            new FormationSlot("ST",   "ST",  "FW",  0.50f, 0.20f),
            new FormationSlot("RW",   "RW",  "FW",  0.85f, 0.20f)
        ));
        FORMATIONS.put("3-5-2", Arrays.asList(
            new FormationSlot("GK",   "GK",  "GK",  0.50f, 0.85f),
            new FormationSlot("CB1",  "CB",  "DF",  0.25f, 0.72f),
            new FormationSlot("CB2",  "CB",  "DF",  0.50f, 0.72f),
            new FormationSlot("CB3",  "CB",  "DF",  0.75f, 0.72f),
            new FormationSlot("LM",   "LM",  "MF",  0.08f, 0.52f),
            new FormationSlot("CM1",  "CM",  "MF",  0.28f, 0.52f),
            new FormationSlot("CM2",  "CM",  "MF",  0.50f, 0.52f),
            new FormationSlot("CM3",  "CM",  "MF",  0.72f, 0.52f),
            new FormationSlot("RM",   "RM",  "MF",  0.92f, 0.52f),
            new FormationSlot("ST1",  "ST",  "FW",  0.33f, 0.20f),
            new FormationSlot("ST2",  "ST",  "FW",  0.67f, 0.20f)
        ));
        FORMATIONS.put("4-2-3-1", Arrays.asList(
            new FormationSlot("GK",   "GK",  "GK",  0.50f, 0.85f),
            new FormationSlot("LB",   "LB",  "DF",  0.10f, 0.74f),
            new FormationSlot("CB1",  "CB",  "DF",  0.33f, 0.74f),
            new FormationSlot("CB2",  "CB",  "DF",  0.67f, 0.74f),
            new FormationSlot("RB",   "RB",  "DF",  0.90f, 0.74f),
            new FormationSlot("CDM1", "CDM", "MF",  0.35f, 0.58f),
            new FormationSlot("CDM2", "CDM", "MF",  0.65f, 0.58f),
            new FormationSlot("LAM",  "LAM", "MF",  0.18f, 0.38f),
            new FormationSlot("CAM",  "CAM", "MF",  0.50f, 0.38f),
            new FormationSlot("RAM",  "RAM", "MF",  0.82f, 0.38f),
            new FormationSlot("ST",   "ST",  "FW",  0.50f, 0.18f)
        ));
    }

    public static List<FormationSlot> get(String name) {
        List<FormationSlot> slots = FORMATIONS.get(name);
        return slots != null ? slots : FORMATIONS.get(DEFAULT);
    }

    public static List<String> names() {
        return new ArrayList<>(FORMATIONS.keySet());
    }

    public static boolean matchesPosGroup(String pos, String group) {
        if (pos == null || group == null) return false;
        switch (group) {
            case "GK": return pos.equals("GK");
            case "DF": return pos.equals("CB") || pos.equals("RB") || pos.equals("LB");
            case "MF": return pos.equals("CM") || pos.equals("CDM") || pos.equals("CAM")
                           || pos.equals("LM") || pos.equals("RM")
                           || pos.equals("LAM") || pos.equals("RAM");
            case "FW": return pos.equals("ST") || pos.equals("FW")
                           || pos.equals("LW") || pos.equals("RW");
            default:   return false;
        }
    }

    private FormationCatalog() {}
}

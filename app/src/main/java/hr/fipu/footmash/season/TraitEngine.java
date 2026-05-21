package hr.fipu.footmash.season;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.SynergyResult;
import hr.fipu.footmash.model.Trait;

/**
 * Auto-derives player traits from attributes and evaluates how a starting XI's
 * traits combine. All logic is pure (no Android, no DB) so it is unit-testable
 * and cheap to call repeatedly during drafting and simulation.
 */
public final class TraitEngine {

    private TraitEngine() {}

    /** Minimum fit score (0–99-ish) for a trait to be awarded beyond the player's best one. */
    private static final int TRAIT_THRESHOLD = 62;
    /** A player keeps at most this many traits. */
    private static final int MAX_TRAITS = 3;

    // ─── Trait derivation ──────────────────────────────────────────────────────

    /**
     * Returns the player's traits, strongest first, capped at {@link #MAX_TRAITS}.
     * Always returns at least one trait for a non-null player.
     */
    public static List<Trait> deriveTraits(RealPlayer p) {
        List<Trait> result = new ArrayList<>();
        if (p == null) return result;

        String group = groupOf(p.getPosition());
        List<Trait> eligible = new ArrayList<>();
        final Map<Trait, Integer> fit = new HashMap<>();
        for (Trait t : Trait.values()) {
            if (t.group.equals(group)) {
                eligible.add(t);
                fit.put(t, fitScore(t, p));
            }
        }
        eligible.sort((a, b) -> Integer.compare(fit.get(b), fit.get(a)));

        for (Trait t : eligible) {
            if (result.size() >= MAX_TRAITS) break;
            if (fit.get(t) >= TRAIT_THRESHOLD) result.add(t);
        }
        // Guarantee every player has at least their single defining trait.
        if (result.isEmpty() && !eligible.isEmpty()) result.add(eligible.get(0));
        return result;
    }

    /** Maps a seed position string to one of "GK", "DF", "MF", "FW". */
    public static String groupOf(String position) {
        if (position == null) return "MF";
        switch (position) {
            case "GK":
                return "GK";
            case "CB": case "RB": case "LB":
                return "DF";
            case "ST": case "FW": case "LW": case "RW":
                return "FW";
            default: // CM, CDM, CAM, LM, RM, LAM, RAM
                return "MF";
        }
    }

    private static int fitScore(Trait t, RealPlayer p) {
        int pace = p.getPace(), sho = p.getShooting(), pas = p.getPassing();
        int dri = p.getDribbling(), def = p.getDefending(), phy = p.getPhysical();
        int ovr = p.getOverall();
        boolean fullback = "RB".equals(p.getPosition()) || "LB".equals(p.getPosition());

        switch (t) {
            // Goalkeepers: the six attributes don't model reflexes, so the core
            // shot-stopping trait keys off overall instead.
            case SHOT_STOPPER:      return ovr;
            case SWEEPER_KEEPER:    return Math.round(pace * 0.6f + pas * 0.4f);
            case PLAYMAKER_GK:      return pas;
            case AERIAL_COMMANDER:  return phy;

            case BALL_PLAYING_DEF:  return pas;
            case NO_NONSENSE_DEF:   return Math.round(def * 0.55f + phy * 0.45f)
                                           - Math.max(0, pas - 72) / 2;
            case OVERLAPPING_FB:    return fullback ? pace : Math.round(pace * 0.5f);
            case AERIAL_WALL:       return phy;
            case LAST_DITCH:        return Math.round(def * 0.7f + pace * 0.3f);

            case PLAYMAKER:         return Math.round(pas * 0.55f + dri * 0.45f);
            case BOX_TO_BOX:        return Math.round(phy * 0.4f + def * 0.3f + sho * 0.3f);
            case CURVED_CROSSER:    return Math.round(pas * 0.5f + pace * 0.5f);
            case BALL_WINNER:       return def;
            case TEMPO_SETTER:      return pas - Math.round(Math.max(0, pace - 72) * 0.4f);

            case GOAL_POACHER:      return sho - Math.round(Math.max(0, pas - 68) * 0.4f);
            case FALSE_NINE:        return Math.round(pas * 0.5f + dri * 0.5f);
            case TARGET_MAN:        return Math.round(phy * 0.55f + sho * 0.45f);
            case SPEED_MERCHANT:    return pace;
            case CLINICAL_FINISHER: return sho;
            default:                return 0;
        }
    }

    // ─── Synergy table ─────────────────────────────────────────────────────────

    private static final class Combo {
        final int weight;
        final String label;
        Combo(int weight, String label) { this.weight = weight; this.label = label; }
    }

    private static final Map<String, Combo> SYNERGY = new HashMap<>();

    private static void syn(Trait a, Trait b, int weight, String label) {
        SYNERGY.put(key(a, b), new Combo(weight, label));
    }

    private static String key(Trait a, Trait b) {
        return a.ordinal() < b.ordinal()
                ? a.name() + "|" + b.name()
                : b.name() + "|" + a.name();
    }

    static {
        // Positive combinations (++ = 3, + = 2).
        syn(Trait.CURVED_CROSSER, Trait.GOAL_POACHER,    3, "Majstor centra → Lovac na golove");
        syn(Trait.CURVED_CROSSER, Trait.TARGET_MAN,      3, "Majstor centra → Klasična devetka");
        syn(Trait.CURVED_CROSSER, Trait.AERIAL_WALL,     2, "Centaršut + Zračna prijetnja");
        syn(Trait.PLAYMAKER,      Trait.GOAL_POACHER,    3, "Kreator igre → Lovac na golove");
        syn(Trait.PLAYMAKER,      Trait.SPEED_MERCHANT,  3, "Kreator igre → Raketa (ubačaj iza obrane)");
        syn(Trait.PLAYMAKER,      Trait.FALSE_NINE,      2, "Kreator igre + Lažna devetka");
        syn(Trait.BALL_PLAYING_DEF, Trait.PLAYMAKER,     3, "Stoper s loptom → Kreator igre");
        syn(Trait.BALL_PLAYING_DEF, Trait.TEMPO_SETTER,  2, "Stoper s loptom + Dirigent");
        syn(Trait.BALL_WINNER,    Trait.BALL_PLAYING_DEF,2, "Otimač lopti + Stoper s loptom");
        syn(Trait.BALL_WINNER,    Trait.PLAYMAKER,       2, "Otimač lopti osvaja, Kreator gradi");
        syn(Trait.SWEEPER_KEEPER, Trait.OVERLAPPING_FB,  2, "Pometač + Napadački bek (visoka linija)");
        syn(Trait.PLAYMAKER_GK,   Trait.SPEED_MERCHANT,  3, "Pokretač akcije → Raketa (kontranapad)");
        syn(Trait.BOX_TO_BOX,     Trait.TARGET_MAN,      2, "Box-to-box + Klasična devetka");
        syn(Trait.SHOT_STOPPER,   Trait.NO_NONSENSE_DEF, 2, "Sjajan refleks + Čvrsti stoper");
        syn(Trait.OVERLAPPING_FB, Trait.GOAL_POACHER,    2, "Napadački bek → Lovac na golove");
        syn(Trait.OVERLAPPING_FB, Trait.TARGET_MAN,      2, "Napadački bek → Klasična devetka");

        // Negative combinations (- = -2, -- = -3).
        syn(Trait.GOAL_POACHER,   Trait.FALSE_NINE,     -3, "Lovac na golove i Lažna devetka traže isti prostor");
        syn(Trait.FALSE_NINE,     Trait.TARGET_MAN,     -2, "Lažna i Klasična devetka — sukob uloga");
        syn(Trait.SWEEPER_KEEPER, Trait.NO_NONSENSE_DEF,-3, "Pometač traži visoku liniju, Čvrsti stoper duboki blok");
        syn(Trait.TEMPO_SETTER,   Trait.SPEED_MERCHANT, -2, "Dirigent usporava, Raketa želi trčati");
    }

    // ─── Synergy evaluation ────────────────────────────────────────────────────

    /** Evaluates every trait pairing across the given XI (or partial lineup). */
    public static SynergyResult computeSynergy(List<RealPlayer> xi) {
        if (xi == null || xi.size() < 2) return SynergyResult.empty();

        List<List<Trait>> traits = new ArrayList<>();
        for (RealPlayer p : xi) traits.add(deriveTraits(p));

        int raw = 0;
        Map<String, Integer> firedPos = new LinkedHashMap<>();
        Map<String, Integer> firedNeg = new LinkedHashMap<>();

        for (int i = 0; i < xi.size(); i++) {
            for (int j = i + 1; j < xi.size(); j++) {
                for (Trait ta : traits.get(i)) {
                    for (Trait tb : traits.get(j)) {
                        Combo c = SYNERGY.get(key(ta, tb));
                        if (c == null) continue;
                        raw += c.weight;
                        Map<String, Integer> bucket = c.weight >= 0 ? firedPos : firedNeg;
                        Integer count = bucket.get(c.label);
                        bucket.put(c.label, count == null ? 1 : count + 1);
                    }
                }
            }
        }

        int delta = clamp(Math.round(raw / 4.0f), -5, 6);
        int rating = clamp(Math.round(55 + raw * 1.5f), 5, 99);
        return new SynergyResult(rating, delta, labels(firedPos), labels(firedNeg));
    }

    private static List<String> labels(Map<String, Integer> fired) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : fired.entrySet()) {
            out.add(e.getValue() > 1 ? e.getKey() + "  ×" + e.getValue() : e.getKey());
        }
        return out;
    }

    // ─── Effective rating helpers ──────────────────────────────────────────────

    /**
     * Picks a plausible best XI (1 GK, 4 DF, 3 MF, 3 FW) from a full roster,
     * preferring the highest-rated player available for each slot.
     */
    public static List<RealPlayer> bestXi(List<RealPlayer> roster) {
        List<RealPlayer> out = new ArrayList<>();
        if (roster == null || roster.isEmpty()) return out;

        List<RealPlayer> sorted = new ArrayList<>(roster);
        sorted.sort((a, b) -> Integer.compare(b.getOverall(), a.getOverall()));

        String[] groups = {"GK", "DF", "MF", "FW"};
        int[] quota = {1, 4, 3, 3};
        boolean[] taken = new boolean[sorted.size()];

        for (int g = 0; g < groups.length; g++) {
            int need = quota[g];
            for (int i = 0; i < sorted.size() && need > 0; i++) {
                if (taken[i]) continue;
                if (groupOf(sorted.get(i).getPosition()).equals(groups[g])) {
                    out.add(sorted.get(i));
                    taken[i] = true;
                    need--;
                }
            }
        }
        // Fill any shortfall (e.g. a thin roster) with the best remaining players.
        for (int i = 0; i < sorted.size() && out.size() < 11; i++) {
            if (!taken[i]) {
                out.add(sorted.get(i));
                taken[i] = true;
            }
        }
        return out;
    }

    /** Average rating of the XI (including in-season form), or 75 if empty. */
    public static int avgOverall(List<RealPlayer> xi) {
        if (xi == null || xi.isEmpty()) return 75;
        int sum = 0;
        for (RealPlayer p : xi) sum += p.getEffectiveOverall();
        return sum / xi.size();
    }

    /** Effective rating = average overall + trait synergy delta. */
    public static int effectiveRating(List<RealPlayer> xi) {
        return avgOverall(xi) + computeSynergy(xi).delta;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}

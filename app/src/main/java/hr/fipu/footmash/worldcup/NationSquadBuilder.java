package hr.fipu.footmash.worldcup;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import hr.fipu.footmash.db.RealPlayerDao;
import hr.fipu.footmash.model.FormationSlot;
import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.season.FormationCatalog;
import hr.fipu.footmash.season.TraitEngine;

/**
 * Assembles a nation's World Cup pool from the bundled player datasets, topping
 * up thin pools with generated filler players so every nation can field a full
 * 23-man squad. Also provides auto-XI selection and a national strength rating
 * for AI opponents.
 *
 * <p>The bundled JSON only covers five European leagues, so most non-European
 * nations have few (or zero) real players — fillers keep the tournament playable
 * without pretending those squads are real.
 */
public final class NationSquadBuilder {

    /** Minimum pool composition so a valid, position-balanced 23 is always pickable. */
    private static final int MIN_GK = 3, MIN_DF = 8, MIN_MF = 8, MIN_FW = 6;

    private static final String[] FIRST = {
        "Luka", "Ivan", "Marko", "Mateo", "David", "Karlo", "Filip", "Josip", "Antonio",
        "Leo", "Lucas", "Marc", "Hugo", "Daniel", "Diego", "Alex", "Thomas", "James",
        "Antoine", "Pierre", "Mario", "Alessandro", "Lorenzo", "Gabriel", "Arthur",
        "Omar", "Yusuf", "Karim", "Hassan", "Ali", "Kenji", "Hiro", "Min", "Jae",
        "Carlos", "Juan", "Pedro", "Mohamed", "Samuel", "Victor", "Andre", "Noah"
    };
    private static final String[] LAST = {
        "Horvat", "Kovač", "Babić", "Marić", "Novak", "García", "Rodríguez", "González",
        "Fernández", "López", "Martínez", "Sánchez", "Smith", "Jones", "Miller", "Brown",
        "Müller", "Schmidt", "Fischer", "Dubois", "Laurent", "Rossi", "Bianchi", "Silva",
        "Santos", "Almeida", "Traoré", "Diallo", "Okafor", "Mensah", "Tanaka", "Kim",
        "Park", "Hassan", "Haddad", "Khan", "Pérez", "Torres", "Vargas", "Costa"
    };

    private NationSquadBuilder() {}

    /**
     * The full selectable pool for a nation: every real player of that nationality
     * (best first) followed by generated fillers so each line has enough depth for
     * a 23-man squad. Deterministic per nation.
     */
    public static List<WcPlayer> buildPool(RealPlayerDao dao, WorldCupData.Nation nation) {
        List<WcPlayer> pool = new ArrayList<>();
        if (dao != null && nation != null && !nation.englishNames.isEmpty()) {
            List<RealPlayer> real = dao.getPlayersByNationalityInSync(nation.englishNames);
            if (real != null) {
                // The bundled leagues are snapshots from different seasons, so a
                // player who transferred (e.g. Modrić: Real Madrid → AC Milan)
                // appears in two files. Collapse same-name duplicates, keeping the
                // first (highest overall, since the query is ordered desc) so a
                // nation never lists the same person twice.
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (RealPlayer p : real) {
                    String key = p.getName() == null ? "" : p.getName().trim().toLowerCase();
                    if (seen.add(key)) pool.add(WcPlayer.fromReal(p));
                }
            }
        }
        addFillers(pool, nation);
        return pool;
    }

    private static void addFillers(List<WcPlayer> pool, WorldCupData.Nation nation) {
        int gk = 0, df = 0, mf = 0, fw = 0;
        for (WcPlayer p : pool) {
            switch (TraitEngine.groupOf(p.position)) {
                case "GK": gk++; break;
                case "DF": df++; break;
                case "FW": fw++; break;
                default:   mf++; break;
            }
        }
        Random r = new Random(nation == null ? 0 : nation.key.hashCode());
        int[] nextId = { -1 };
        int base = nation == null ? 70 : nation.baseline;
        genGroup(pool, MIN_GK - gk, new String[]{"GK"}, base, r, nextId);
        genGroup(pool, MIN_DF - df, new String[]{"CB", "RB", "LB", "CB"}, base, r, nextId);
        genGroup(pool, MIN_MF - mf, new String[]{"CM", "CDM", "CAM", "CM"}, base, r, nextId);
        genGroup(pool, MIN_FW - fw, new String[]{"ST", "LW", "RW", "ST"}, base, r, nextId);
    }

    private static void genGroup(List<WcPlayer> pool, int count, String[] positions,
                                 int base, Random r, int[] nextId) {
        for (int i = 0; i < count; i++) {
            String pos = positions[i % positions.length];
            int ovr = clamp(base - 2 - r.nextInt(8), 48, Math.min(base, 88));
            pool.add(new WcPlayer(nextId[0]--, randomName(r), pos, ovr, true));
        }
    }

    private static String randomName(Random r) {
        return FIRST[r.nextInt(FIRST.length)] + " " + LAST[r.nextInt(LAST.length)];
    }

    /**
     * Picks a starting XI from the given squad aligned to the formation's slots:
     * the best available player per slot's position group, falling back to the best
     * remaining player. Returns a list the same length as the formation (≤ 11).
     */
    public static List<WcPlayer> autoXi(List<WcPlayer> squad, String formation) {
        List<FormationSlot> slots = FormationCatalog.get(formation);
        List<WcPlayer> sorted = new ArrayList<>(squad);
        sorted.sort((a, b) -> Integer.compare(b.overall, a.overall));
        boolean[] taken = new boolean[sorted.size()];
        List<WcPlayer> xi = new ArrayList<>();
        for (FormationSlot slot : slots) {
            int pick = -1;
            for (int i = 0; i < sorted.size(); i++) {
                if (taken[i]) continue;
                if (TraitEngine.groupOf(sorted.get(i).position).equals(slot.posGroup)) {
                    pick = i;
                    break;
                }
            }
            if (pick < 0) {
                for (int i = 0; i < sorted.size(); i++) {
                    if (!taken[i]) { pick = i; break; }
                }
            }
            if (pick >= 0) { taken[pick] = true; xi.add(sorted.get(pick)); }
        }
        return xi;
    }

    /** Effective rating (avg overall + trait synergy) of an XI of {@link WcPlayer}s. */
    public static int ratingOf(List<WcPlayer> xi) {
        return TraitEngine.effectiveRating(toReal(xi));
    }

    /** Real-player views for the rating/simulation engine. */
    public static List<RealPlayer> toReal(List<WcPlayer> players) {
        List<RealPlayer> out = new ArrayList<>();
        if (players != null) for (WcPlayer p : players) out.add(p.toReal());
        return out;
    }

    /**
     * National strength for an AI nation: the stronger of its FIFA-style baseline
     * and the effective rating of its best available XI (so deep real squads play
     * up to their talent). Clamped to a sane band.
     */
    public static int nationRating(RealPlayerDao dao, WorldCupData.Nation nation) {
        List<WcPlayer> pool = buildPool(dao, nation);
        List<RealPlayer> bestXi = TraitEngine.bestXi(toReal(pool));
        int eff = TraitEngine.effectiveRating(bestXi);
        int base = nation == null ? 70 : nation.baseline;
        return clamp(Math.max(base, eff), 45, 95);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}

package hr.fipu.footmash.worldcup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The 48-nation field for the 2026 FIFA World Cup (hosted by the USA, Canada and
 * Mexico), grouped by confederation. Each nation maps to its bundled crest under
 * {@code assets/icons/worldcup/}, the nationality string(s) used inside the player
 * JSON datasets (so a nation's real squad can be pulled from {@code real_players}),
 * and a baseline national strength used to seed AI opponents. Pure in-memory data
 * — no DB / network.
 */
public final class WorldCupData {

    public static final String TITLE = "Svjetsko prvenstvo 2026";
    public static final String SUBTITLE = "48 reprezentacija · SAD · Kanada · Meksiko";

    private static final String DIR = "icons/worldcup/";
    private static final String SUF = "-national-team.football-logos.cc.png";

    /**
     * A single qualified nation. {@code logo} is an asset-relative path,
     * {@code key} is a stable identifier (the crest slug), {@code englishNames}
     * are the nationality values present in the bundled JSON, and
     * {@code baseline} is the national-team strength (≈45–92) used to rate AI
     * sides.
     */
    public static final class Nation {
        public final String key;
        public final String name;
        public final String logo;
        public final String confederation;
        public final boolean host;
        public final int baseline;
        public final List<String> englishNames;

        Nation(String key, String name, String logo, String confederation,
               boolean host, int baseline, String... englishNames) {
            this.key = key;
            this.name = name;
            this.logo = logo;
            this.confederation = confederation;
            this.host = host;
            this.baseline = baseline;
            this.englishNames = Collections.unmodifiableList(Arrays.asList(englishNames));
        }
    }

    /** Crest filename = {@code key + SUF}. */
    private static Nation n(String key, String name, String conf, boolean host,
                            int baseline, String... english) {
        return new Nation(key, name, DIR + key + SUF, conf, host, baseline, english);
    }

    /** Crest filename supplied explicitly (when it doesn't follow the slug pattern). */
    private static Nation nLogo(String key, String name, String logoFile, String conf,
                                boolean host, int baseline, String... english) {
        return new Nation(key, name, DIR + logoFile, conf, host, baseline, english);
    }

    private static final List<Nation> NATIONS = Collections.unmodifiableList(Arrays.asList(
        // ── Domaćini & CONCACAF ──
        n("usa", "Sjedinjene Države", "CONCACAF", true, 78, "USA", "United States"),
        n("canada", "Kanada", "CONCACAF", true, 76, "Canada"),
        n("mexico", "Meksiko", "CONCACAF", true, 78, "Mexico"),
        n("panama", "Panama", "CONCACAF", false, 70, "Panama"),
        n("haiti", "Haiti", "CONCACAF", false, 66, "Haiti"),
        nLogo("curacao", "Curaçao", "curacao" + SUF, "CONCACAF", false, 66, "Curaçao", "Curacao"),

        // ── Europa (UEFA) ──
        n("austria", "Austrija", "UEFA", false, 79, "Austria"),
        n("belgium", "Belgija", "UEFA", false, 85, "Belgium"),
        n("bosnia-and-herzegovina", "Bosna i Hercegovina", "UEFA", false, 75,
            "Bosnia and Herzegovina", "Bosnia"),
        n("croatia", "Hrvatska", "UEFA", false, 85, "Croatia"),
        n("czech-republic", "Češka", "UEFA", false, 77, "Czech Republic", "Czechia"),
        n("dutch", "Nizozemska", "UEFA", false, 88, "Netherlands"),
        n("england", "Engleska", "UEFA", false, 89, "England"),
        n("france", "Francuska", "UEFA", false, 91, "France"),
        n("germany", "Njemačka", "UEFA", false, 86, "Germany"),
        n("norway", "Norveška", "UEFA", false, 80, "Norway"),
        nLogo("portugal", "Portugal", "portuguese-football-federation.football-logos.cc.png",
            "UEFA", false, 89, "Portugal"),
        n("scotland", "Škotska", "UEFA", false, 75, "Scotland"),
        n("spain", "Španjolska", "UEFA", false, 90, "Spain"),
        n("sweden", "Švedska", "UEFA", false, 76, "Sweden"),
        n("switzerland", "Švicarska", "UEFA", false, 80, "Switzerland"),
        n("turkey", "Turska", "UEFA", false, 78, "Turkey", "Türkiye"),

        // ── Južna Amerika (CONMEBOL) ──
        n("argentina", "Argentina", "CONMEBOL", false, 92, "Argentina"),
        n("brazil", "Brazil", "CONMEBOL", false, 89, "Brazil"),
        n("colombia", "Kolumbija", "CONMEBOL", false, 83, "Colombia"),
        n("ecuador", "Ekvador", "CONMEBOL", false, 78, "Ecuador"),
        n("paraguay", "Paragvaj", "CONMEBOL", false, 74, "Paraguay"),
        n("uruguay", "Urugvaj", "CONMEBOL", false, 84, "Uruguay"),

        // ── Afrika (CAF) ──
        n("algeria", "Alžir", "CAF", false, 77, "Algeria"),
        n("cabo-verde", "Zelenortski Otoci", "CAF", false, 70, "Cabo Verde", "Cape Verde"),
        n("congo-dr", "DR Kongo", "CAF", false, 74,
            "DR Congo", "D.R. Congo", "Congo DR", "Congo"),
        n("cote-d-ivoire", "Obala Bjelokosti", "CAF", false, 78,
            "Ivory Coast", "Côte d'Ivoire", "Cote d'Ivoire"),
        n("egypt", "Egipat", "CAF", false, 78, "Egypt"),
        n("ghana", "Gana", "CAF", false, 76, "Ghana"),
        n("morocco", "Maroko", "CAF", false, 84, "Morocco"),
        n("senegal", "Senegal", "CAF", false, 82, "Senegal"),
        n("south-africa", "Južna Afrika", "CAF", false, 73, "South Africa"),
        n("tunisia", "Tunis", "CAF", false, 74, "Tunisia"),

        // ── Azija (AFC) ──
        n("australia", "Australija", "AFC", false, 76, "Australia"),
        n("iran", "Iran", "AFC", false, 77, "Iran", "IR Iran"),
        n("iraq", "Irak", "AFC", false, 70, "Iraq"),
        n("japan", "Japan", "AFC", false, 81, "Japan"),
        n("jordan", "Jordan", "AFC", false, 70, "Jordan"),
        n("qatar", "Katar", "AFC", false, 72, "Qatar"),
        n("saudi-arabia", "Saudijska Arabija", "AFC", false, 73, "Saudi Arabia"),
        n("south-korea", "Južna Koreja", "AFC", false, 78, "South Korea", "Korea Republic"),
        n("uzbekistan", "Uzbekistan", "AFC", false, 72, "Uzbekistan"),

        // ── Oceanija (OFC) ──
        n("new-zealand", "Novi Zeland", "OFC", false, 68, "New Zealand")
    ));

    private static final Map<String, Nation> BY_KEY;
    static {
        Map<String, Nation> m = new LinkedHashMap<>();
        for (Nation nation : NATIONS) m.put(nation.key, nation);
        BY_KEY = Collections.unmodifiableMap(m);
    }

    /** All 48 nations in seeding order. */
    public static List<Nation> all() {
        return NATIONS;
    }

    /** Looks up a nation by its stable key, or {@code null}. */
    public static Nation byKey(String key) {
        return key == null ? null : BY_KEY.get(key);
    }

    /** Nations grouped by confederation, preserving the declared order. */
    public static Map<String, List<Nation>> byConfederation() {
        Map<String, List<Nation>> map = new LinkedHashMap<>();
        for (Nation nation : NATIONS) {
            List<Nation> bucket = map.get(nation.confederation);
            if (bucket == null) {
                bucket = new ArrayList<>();
                map.put(nation.confederation, bucket);
            }
            bucket.add(nation);
        }
        return map;
    }

    public static int count() {
        return NATIONS.size();
    }

    private WorldCupData() {}
}

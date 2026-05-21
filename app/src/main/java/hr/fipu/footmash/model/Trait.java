package hr.fipu.footmash.model;

/**
 * A player playstyle trait. Traits are auto-derived from a player's position and
 * six attributes by {@code TraitEngine} — they are never stored in the database.
 *
 * Each trait belongs to one position group ("GK", "DF", "MF", "FW"); a player can
 * only earn traits from their own group's pool. The value of a trait comes from how
 * it pairs with traits on teammates — see {@code TraitEngine.computeSynergy}.
 */
public enum Trait {

    // ─── Goalkeeper ────────────────────────────────────────────────────────────
    SHOT_STOPPER("Sjajan refleks", "Izvanredne reakcije i obrane udaraca.", "GK"),
    SWEEPER_KEEPER("Pometač", "Izlazi iz gola i pokriva prostor iza obrane.", "GK"),
    PLAYMAKER_GK("Pokretač akcije", "Precizna i dugačka distribucija lopte.", "GK"),
    AERIAL_COMMANDER("Gospodar peterca", "Dominira u kaznenom prostoru kod centaršuteva.", "GK"),

    // ─── Defender ──────────────────────────────────────────────────────────────
    BALL_PLAYING_DEF("Stoper s loptom", "Gradi napad iz zadnje linije.", "DF"),
    NO_NONSENSE_DEF("Čvrsti stoper", "Jednostavna, bezkompromisna obrana.", "DF"),
    OVERLAPPING_FB("Napadački bek", "Trči po boku i ulazi u napad.", "DF"),
    AERIAL_WALL("Zračna prijetnja", "Dominantan u skok-igri na oba kraja terena.", "DF"),
    LAST_DITCH("Spasitelj", "Klizeći startovi i povratno trčanje u zadnji tren.", "DF"),

    // ─── Midfielder ────────────────────────────────────────────────────────────
    PLAYMAKER("Kreator igre", "Diktira napad preciznim dodavanjima.", "MF"),
    BOX_TO_BOX("Box-to-box", "Pokriva cijeli teren — napada i brani.", "MF"),
    CURVED_CROSSER("Majstor centra", "Ubacuje opasne zaobljene centaršuteve.", "MF"),
    BALL_WINNER("Otimač lopti", "Agresivno osvaja loptu u sredini terena.", "MF"),
    TEMPO_SETTER("Dirigent", "Smiruje i kontrolira ritam igre.", "MF"),

    // ─── Forward ───────────────────────────────────────────────────────────────
    GOAL_POACHER("Lovac na golove", "Živi u kaznenom prostoru i zabija iz prilike.", "FW"),
    FALSE_NINE("Lažna devetka", "Spušta se u vezni red i povlači obrane iz pozicije.", "FW"),
    TARGET_MAN("Klasična devetka", "Snažan napadač koji drži loptu i dobiva skok.", "FW"),
    SPEED_MERCHANT("Raketa", "Razara obrane čistom brzinom.", "FW"),
    CLINICAL_FINISHER("Realizator", "Hladnokrvno pretvara prilike u golove.", "FW");

    /** Short Croatian name, suitable for a chip. */
    public final String label;
    /** One-line Croatian description. */
    public final String description;
    /** Position group this trait belongs to: "GK", "DF", "MF" or "FW". */
    public final String group;

    Trait(String label, String description, String group) {
        this.label = label;
        this.description = description;
        this.group = group;
    }
}

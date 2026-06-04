package hr.fipu.footmash.worldcup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The full state of a World Cup playthrough, serialized as a single JSON blob
 * (Gson) in the {@code world_cup} table. Plain public fields and no-arg
 * constructors keep serialization trivial. The {@link WorldCupRepository} owns
 * all mutations.
 */
public class WcTournament {

    // Stage values for {@link #stage}.
    public static final String STAGE_SQUAD = "SQUAD";
    public static final String STAGE_GROUP = "GROUP";
    public static final String STAGE_R32   = "R32";
    public static final String STAGE_R16   = "R16";
    public static final String STAGE_QF    = "QF";
    public static final String STAGE_SF    = "SF";
    public static final String STAGE_FINAL = "FINAL";
    public static final String STAGE_DONE  = "DONE";

    public String userNationKey;
    public String formation = "4-3-3";
    public List<WcPlayer> squad = new ArrayList<>();   // 23 (real + fillers)
    public List<Integer> startingXiIds = new ArrayList<>(); // ids into squad, the XI

    public List<WcGroup> groups = new ArrayList<>();   // 12 (A..L)
    public List<WcMatch> groupFixtures = new ArrayList<>();
    public List<WcRound> knockout = new ArrayList<>();

    public String stage = STAGE_SQUAD;
    public int groupMatchday = 1;   // next group matchday to play (1..3)

    public String championKey;
    public String runnerUpKey;
    public String thirdKey;

    /** nationKey → effective rating (set at draw time). */
    public Map<String, Integer> ratings;
    /** nationKey → group letter. */
    public Map<String, String> groupByNation;

    public boolean groupStageComplete() {
        return groupMatchday > 3;
    }

    // ── Nested types ──────────────────────────────────────────────────────────

    public static class WcGroup {
        public String name;                       // "A".."L"
        public List<String> nationKeys = new ArrayList<>();
        public List<WcStanding> table = new ArrayList<>();
    }

    public static class WcStanding {
        public String nationKey;
        public int played, won, drawn, lost, gf, ga, pts;

        public WcStanding() {}
        public WcStanding(String nationKey) { this.nationKey = nationKey; }

        public int gd() { return gf - ga; }
    }

    /** A group fixture or a knockout tie. */
    public static class WcMatch {
        public String group;     // group letter (group stage) or null (knockout)
        public int matchday;     // 1..3 for group stage; 0 for knockout
        public String homeKey;
        public String awayKey;
        public boolean played;
        public int homeGoals;
        public int awayGoals;
        public boolean penalties;
        public int homePens;
        public int awayPens;
        public String winnerKey; // knockout only
        public List<WcScorer> scorers = new ArrayList<>();

        public WcMatch() {}
        public WcMatch(String homeKey, String awayKey) {
            this.homeKey = homeKey;
            this.awayKey = awayKey;
        }
    }

    public static class WcScorer {
        public String name;
        public String nationKey;
        public int minute;
        public String assist;
    }

    public static class WcRound {
        public String stage;                 // R32, R16, QF, SF, FINAL
        public List<WcMatch> ties = new ArrayList<>();
        public boolean played;

        public WcRound() {}
        public WcRound(String stage) { this.stage = stage; }
    }
}

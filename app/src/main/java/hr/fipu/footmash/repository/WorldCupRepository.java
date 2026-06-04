package hr.fipu.footmash.repository;

import android.content.Context;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import hr.fipu.footmash.ai.GeminiRepository;
import hr.fipu.footmash.ai.LocalSimulator;
import hr.fipu.footmash.ai.MatchSimulator;
import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.db.RealPlayerDao;
import hr.fipu.footmash.db.WorldCupDao;
import hr.fipu.footmash.model.Fixture;
import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.Trait;
import hr.fipu.footmash.model.WorldCupState;
import hr.fipu.footmash.season.TraitEngine;
import hr.fipu.footmash.worldcup.NationSquadBuilder;
import hr.fipu.footmash.worldcup.WcPlayer;
import hr.fipu.footmash.worldcup.WcTournament;
import hr.fipu.footmash.worldcup.WorldCupData;

/**
 * Drives a World Cup playthrough: draws the 48-nation field into 12 groups,
 * simulates each group matchday and knockout round, and advances the bracket
 * through to a champion. Match outcomes reuse the season engine — Gemini first
 * ({@link MatchSimulator}), then the offline {@link LocalSimulator} fallback —
 * mirroring {@link SeasonRepository#simulateMatchday}.
 *
 * <p>All methods that touch the database must run on a background thread.
 */
public class WorldCupRepository {

    private final WorldCupDao worldCupDao;
    private final RealPlayerDao realPlayerDao;
    private final GeminiRepository gemini;
    private final Gson gson = new Gson();
    private final Random rng = new Random();

    public WorldCupRepository(AppDatabase db, GeminiRepository gemini) {
        this.worldCupDao = db.worldCupDao();
        this.realPlayerDao = db.realPlayerDao();
        this.gemini = gemini;
    }

    public WorldCupRepository(Context context) {
        this(AppDatabase.getInstance(context.getApplicationContext()), new GeminiRepository());
    }

    // ── State I/O ───────────────────────────────────────────────────────────

    public WcTournament load() {
        WorldCupState row = worldCupDao.getSync();
        if (row == null || row.getJson() == null) return null;
        try {
            return gson.fromJson(row.getJson(), WcTournament.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void save(WcTournament t) {
        WorldCupState row = new WorldCupState();
        row.setJson(gson.toJson(t));
        worldCupDao.upsert(row);
    }

    public void clear() {
        worldCupDao.clear();
    }

    public boolean hasActive() {
        return worldCupDao.getSync() != null;
    }

    /** The selectable pool (real + filler) for a nation, used by the squad picker. */
    public List<WcPlayer> poolFor(String nationKey) {
        return NationSquadBuilder.buildPool(realPlayerDao, WorldCupData.byKey(nationKey));
    }

    /** nationKey → number of real (non-filler) players available, for the picker. */
    public Map<String, Integer> realPlayerCounts() {
        Map<String, Integer> m = new HashMap<>();
        for (WorldCupData.Nation n : WorldCupData.all()) {
            int c = 0;
            if (!n.englishNames.isEmpty()) {
                List<RealPlayer> r = realPlayerDao.getPlayersByNationalityInSync(n.englishNames);
                c = r == null ? 0 : r.size();
            }
            m.put(n.key, c);
        }
        return m;
    }

    // ── Tournament setup ──────────────────────────────────────────────────────

    /**
     * Creates a fresh tournament: pot-based group draw, per-nation ratings and the
     * round-robin group fixtures. The user's nation uses {@code startingXiIds} for
     * its rating; AI nations use {@link NationSquadBuilder#nationRating}.
     */
    public WcTournament startTournament(String nationKey, String formation,
                                        List<WcPlayer> squad, List<Integer> startingXiIds) {
        WcTournament t = new WcTournament();
        t.userNationKey = nationKey;
        t.formation = formation;
        t.squad = squad;
        t.startingXiIds = startingXiIds;
        t.ratings = new HashMap<>();
        t.groupByNation = new HashMap<>();

        // Ratings for every nation.
        for (WorldCupData.Nation n : WorldCupData.all()) {
            if (n.key.equals(nationKey)) {
                t.ratings.put(n.key, clampRating(NationSquadBuilder.ratingOf(userXi(t))));
            } else {
                t.ratings.put(n.key, clampRating(NationSquadBuilder.nationRating(realPlayerDao, n)));
            }
        }

        drawGroups(t);
        generateGroupFixtures(t);

        t.stage = WcTournament.STAGE_GROUP;
        t.groupMatchday = 1;
        save(t);
        return t;
    }

    /** Seeds nations into 4 pots by rating and draws one per pot into each group. */
    private void drawGroups(WcTournament t) {
        List<String> keys = new ArrayList<>();
        for (WorldCupData.Nation n : WorldCupData.all()) keys.add(n.key);
        keys.sort((a, b) -> Integer.compare(t.ratings.get(b), t.ratings.get(a)));

        // 4 pots of 12, strongest first.
        List<List<String>> pots = new ArrayList<>();
        for (int p = 0; p < 4; p++) {
            List<String> pot = new ArrayList<>(keys.subList(p * 12, p * 12 + 12));
            Collections.shuffle(pot, rng);
            pots.add(pot);
        }

        String[] names = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"};
        for (int g = 0; g < 12; g++) {
            WcTournament.WcGroup grp = new WcTournament.WcGroup();
            grp.name = names[g];
            t.groups.add(grp);
        }
        for (int p = 0; p < 4; p++) {
            for (int g = 0; g < 12; g++) {
                String key = pots.get(p).get(g);
                t.groups.get(g).nationKeys.add(key);
            }
        }
        // Make sure the user's group order is stable and standings rows exist.
        for (WcTournament.WcGroup grp : t.groups) {
            for (String key : grp.nationKeys) {
                grp.table.add(new WcTournament.WcStanding(key));
                t.groupByNation.put(key, grp.name);
            }
        }
    }

    /** Single round-robin (3 matchdays, 2 matches per group per matchday). */
    private void generateGroupFixtures(WcTournament t) {
        // index pairings for a 4-team round robin
        int[][][] sched = {
            {{0, 3}, {1, 2}},  // MD1
            {{0, 2}, {3, 1}},  // MD2
            {{0, 1}, {2, 3}}   // MD3
        };
        for (WcTournament.WcGroup grp : t.groups) {
            List<String> k = grp.nationKeys;
            for (int md = 0; md < 3; md++) {
                for (int[] pair : sched[md]) {
                    WcTournament.WcMatch m = new WcTournament.WcMatch(k.get(pair[0]), k.get(pair[1]));
                    m.group = grp.name;
                    m.matchday = md + 1;
                    t.groupFixtures.add(m);
                }
            }
        }
    }

    // ── Advance the tournament one stage per call ────────────────────────────

    /**
     * Plays the next group matchday or knockout round. Returns the updated state
     * (also persisted), or {@code null} if there is no active/unfinished
     * tournament.
     */
    public WcTournament simulateNextStage(String apiKey) {
        WcTournament t = load();
        if (t == null || WcTournament.STAGE_DONE.equals(t.stage)) return t;

        if (WcTournament.STAGE_GROUP.equals(t.stage)) {
            simulateGroupMatchday(t, t.groupMatchday, apiKey);
            t.groupMatchday++;
            if (t.groupStageComplete()) {
                finalizeGroups(t);
                t.stage = WcTournament.STAGE_R32;
            }
        } else {
            simulateKnockoutRound(t, apiKey);
        }

        save(t);
        return t;
    }

    // ── Group stage ───────────────────────────────────────────────────────────

    private void simulateGroupMatchday(WcTournament t, int matchday, String apiKey) {
        List<WcTournament.WcMatch> matches = new ArrayList<>();
        for (WcTournament.WcMatch m : t.groupFixtures) {
            if (m.matchday == matchday && !m.played) matches.add(m);
        }
        if (matches.isEmpty()) return;
        simulateMatches(t, matches, apiKey, false);
        for (WcTournament.WcGroup g : t.groups) recomputeGroup(t, g);
    }

    private void recomputeGroup(WcTournament t, WcTournament.WcGroup g) {
        Map<String, WcTournament.WcStanding> rows = new HashMap<>();
        for (WcTournament.WcStanding s : g.table) {
            s.played = s.won = s.drawn = s.lost = s.gf = s.ga = s.pts = 0;
            rows.put(s.nationKey, s);
        }
        for (WcTournament.WcMatch m : t.groupFixtures) {
            if (!g.name.equals(m.group) || !m.played) continue;
            applyResult(rows.get(m.homeKey), m.homeGoals, m.awayGoals);
            applyResult(rows.get(m.awayKey), m.awayGoals, m.homeGoals);
        }
        g.table.sort(STANDING_ORDER);
    }

    private static void applyResult(WcTournament.WcStanding s, int gf, int ga) {
        if (s == null) return;
        s.played++;
        s.gf += gf;
        s.ga += ga;
        if (gf > ga)      { s.won++;   s.pts += 3; }
        else if (gf == ga){ s.drawn++; s.pts += 1; }
        else              { s.lost++; }
    }

    private static final java.util.Comparator<WcTournament.WcStanding> STANDING_ORDER =
        (a, b) -> {
            if (b.pts != a.pts) return Integer.compare(b.pts, a.pts);
            if (b.gd() != a.gd()) return Integer.compare(b.gd(), a.gd());
            return Integer.compare(b.gf, a.gf);
        };

    private void finalizeGroups(WcTournament t) {
        List<String> seeds = computeR32Seeds(t.groups);
        WcTournament.WcRound r32 = new WcTournament.WcRound(WcTournament.STAGE_R32);
        for (int i = 0; i < 16; i++) {
            r32.ties.add(new WcTournament.WcMatch(seeds.get(i), seeds.get(31 - i)));
        }
        t.knockout.add(r32);
    }

    /**
     * Ranks every group, then returns the 32 qualifiers in seeding order: the 12
     * group winners (strongest first), the 12 runners-up, and the 8 best
     * third-placed nations (by points, goal difference, goals scored). Pure logic,
     * so it can be unit-tested without a database.
     */
    public static List<String> computeR32Seeds(List<WcTournament.WcGroup> groups) {
        List<WcTournament.WcStanding> winners = new ArrayList<>();
        List<WcTournament.WcStanding> runners = new ArrayList<>();
        List<WcTournament.WcStanding> thirds = new ArrayList<>();
        for (WcTournament.WcGroup g : groups) {
            g.table.sort(STANDING_ORDER);
            winners.add(g.table.get(0));
            runners.add(g.table.get(1));
            thirds.add(g.table.get(2));
        }
        winners.sort(STANDING_ORDER);
        runners.sort(STANDING_ORDER);
        thirds.sort(STANDING_ORDER);

        List<String> seeds = new ArrayList<>();
        for (WcTournament.WcStanding s : winners) seeds.add(s.nationKey);
        for (WcTournament.WcStanding s : runners) seeds.add(s.nationKey);
        for (int i = 0; i < 8; i++) seeds.add(thirds.get(i).nationKey);   // best 8 thirds
        return seeds;
    }

    // ── Knockout stage ────────────────────────────────────────────────────────

    private void simulateKnockoutRound(WcTournament t, String apiKey) {
        WcTournament.WcRound round = currentRound(t);
        if (round == null) return;

        simulateMatches(t, round.ties, apiKey, true);
        for (WcTournament.WcMatch m : round.ties) resolveWinner(t, m);
        round.played = true;

        buildNextRound(t, round);
    }

    private WcTournament.WcRound currentRound(WcTournament t) {
        for (WcTournament.WcRound r : t.knockout) {
            if (!r.played) return r;
        }
        return null;
    }

    private void buildNextRound(WcTournament t, WcTournament.WcRound finished) {
        List<String> winners = new ArrayList<>();
        for (WcTournament.WcMatch m : finished.ties) winners.add(m.winnerKey);

        switch (finished.stage) {
            case WcTournament.STAGE_R32:
                t.knockout.add(pairUp(WcTournament.STAGE_R16, winners));
                t.stage = WcTournament.STAGE_R16;
                break;
            case WcTournament.STAGE_R16:
                t.knockout.add(pairUp(WcTournament.STAGE_QF, winners));
                t.stage = WcTournament.STAGE_QF;
                break;
            case WcTournament.STAGE_QF:
                t.knockout.add(pairUp(WcTournament.STAGE_SF, winners));
                t.stage = WcTournament.STAGE_SF;
                break;
            case WcTournament.STAGE_SF: {
                // Final + third-place playoff (SF losers).
                WcTournament.WcRound finalRound = new WcTournament.WcRound(WcTournament.STAGE_FINAL);
                List<String> losers = new ArrayList<>();
                for (WcTournament.WcMatch m : finished.ties) losers.add(loserOf(m));
                finalRound.ties.add(new WcTournament.WcMatch(losers.get(0), losers.get(1)));   // 3rd place
                finalRound.ties.add(new WcTournament.WcMatch(winners.get(0), winners.get(1))); // final
                t.knockout.add(finalRound);
                t.stage = WcTournament.STAGE_FINAL;
                break;
            }
            case WcTournament.STAGE_FINAL: {
                WcTournament.WcMatch third = finished.ties.get(0);
                WcTournament.WcMatch fin = finished.ties.get(1);
                t.thirdKey = third.winnerKey;
                t.championKey = fin.winnerKey;
                t.runnerUpKey = loserOf(fin);
                t.stage = WcTournament.STAGE_DONE;
                break;
            }
            default:
                break;
        }
    }

    private WcTournament.WcRound pairUp(String stage, List<String> winners) {
        WcTournament.WcRound r = new WcTournament.WcRound(stage);
        for (int i = 0; i + 1 < winners.size(); i += 2) {
            r.ties.add(new WcTournament.WcMatch(winners.get(i), winners.get(i + 1)));
        }
        return r;
    }

    private void resolveWinner(WcTournament t, WcTournament.WcMatch m) {
        if (m.homeGoals > m.awayGoals) {
            m.winnerKey = m.homeKey;
        } else if (m.awayGoals > m.homeGoals) {
            m.winnerKey = m.awayKey;
        } else {
            // Penalty shootout, weighted by rating.
            int rh = rating(t, m.homeKey), ra = rating(t, m.awayKey);
            boolean homeWins = rng.nextDouble() < (double) rh / (rh + ra);
            m.penalties = true;
            int winPens = 4 + rng.nextInt(2);          // 4 or 5
            int losePens = Math.max(2, rng.nextInt(winPens)); // 2..winPens-1
            if (losePens >= winPens) losePens = winPens - 1;
            if (homeWins) { m.homePens = winPens; m.awayPens = losePens; m.winnerKey = m.homeKey; }
            else          { m.awayPens = winPens; m.homePens = losePens; m.winnerKey = m.awayKey; }
        }
    }

    private static String loserOf(WcTournament.WcMatch m) {
        return m.winnerKey == null ? null
            : (m.winnerKey.equals(m.homeKey) ? m.awayKey : m.homeKey);
    }

    // ── Shared simulation core ────────────────────────────────────────────────

    /**
     * Simulates a batch of World Cup matches, writing goals/scorers back onto each
     * {@link WcTournament.WcMatch}. Uses the user's chosen XI for the rich Gemini
     * prompt when their nation features, otherwise the simple prompt; falls back
     * to the local simulator.
     */
    private void simulateMatches(WcTournament t, List<WcTournament.WcMatch> matches,
                                 String apiKey, boolean knockout) {
        Map<String, String> nameToKey = new HashMap<>();
        Map<String, Integer> ratings = new HashMap<>();
        Map<String, List<RealPlayer>> rosters = new HashMap<>();
        List<Fixture> fixtures = new ArrayList<>();
        boolean userInRound = false;

        for (WcTournament.WcMatch m : matches) {
            boolean user = m.homeKey.equals(t.userNationKey) || m.awayKey.equals(t.userNationKey);
            if (user) userInRound = true;
            Fixture f = new Fixture();
            f.setHomeTeamName(displayName(m.homeKey));
            f.setAwayTeamName(displayName(m.awayKey));
            f.setUserTeam(user);
            fixtures.add(f);
            registerNation(t, m.homeKey, nameToKey, ratings, rosters);
            registerNation(t, m.awayKey, nameToKey, ratings, rosters);
        }

        MatchSimulator.UserTeamInfo userInfo = userInRound ? buildUserInfo(t) : null;

        String raw = gemini.callSync(
            userInfo != null
                ? MatchSimulator.buildPrompt(fixtures, userInfo, ratings)
                : MatchSimulator.buildSimplePrompt(fixtures, ratings),
            apiKey);
        List<MatchSimulator.ParsedMatch> results = MatchSimulator.parseResponse(raw, fixtures.size());

        if (results == null) {
            raw = gemini.callSync(MatchSimulator.buildSimplePrompt(fixtures, ratings), apiKey);
            results = MatchSimulator.parseResponse(raw, fixtures.size());
        }
        if (results == null) {
            results = LocalSimulator.simulateAll(fixtures, ratings, rosters);
        }

        for (int i = 0; i < matches.size(); i++) {
            applyMatchResult(t, matches.get(i), results.get(i), knockout);
        }
    }

    private void registerNation(WcTournament t, String key, Map<String, String> nameToKey,
                                Map<String, Integer> ratings, Map<String, List<RealPlayer>> rosters) {
        String name = displayName(key);
        if (ratings.containsKey(name)) return;
        nameToKey.put(name, key);
        ratings.put(name, rating(t, key));
        rosters.put(name, rosterFor(t, key));
    }

    private List<RealPlayer> rosterFor(WcTournament t, String key) {
        if (key.equals(t.userNationKey)) {
            return NationSquadBuilder.toReal(userXi(t));
        }
        return NationSquadBuilder.toReal(
            NationSquadBuilder.buildPool(realPlayerDao, WorldCupData.byKey(key)));
    }

    private void applyMatchResult(WcTournament t, WcTournament.WcMatch m,
                                  MatchSimulator.ParsedMatch pm, boolean knockout) {
        m.homeGoals = pm.homeGoals;
        m.awayGoals = pm.awayGoals;
        m.played = true;
        m.scorers.clear();
        if (pm.scorers != null) {
            for (MatchSimulator.Scorer sc : pm.scorers) {
                WcTournament.WcScorer w = new WcTournament.WcScorer();
                w.name = sc.name;
                w.nationKey = "home".equals(sc.team) ? m.homeKey : m.awayKey;
                w.minute = Math.max(1, Math.min(90, sc.minute));
                w.assist = sc.assist != null ? sc.assist.trim() : null;
                m.scorers.add(w);
            }
        }
    }

    private MatchSimulator.UserTeamInfo buildUserInfo(WcTournament t) {
        List<WcPlayer> xi = userXi(t);
        List<MatchSimulator.PlayerEntry> entries = new ArrayList<>();
        int sum = 0;
        for (WcPlayer p : xi) {
            entries.add(new MatchSimulator.PlayerEntry(
                p.position, p.name, p.overall, traitLabels(p)));
            sum += p.overall;
        }
        int avg = xi.isEmpty() ? 75 : sum / xi.size();
        int synergy = TraitEngine.computeSynergy(NationSquadBuilder.toReal(xi)).delta;
        return new MatchSimulator.UserTeamInfo(
            displayName(t.userNationKey), t.formation, avg, 100, synergy, entries);
    }

    private static String traitLabels(WcPlayer p) {
        StringBuilder sb = new StringBuilder();
        for (Trait tr : TraitEngine.deriveTraits(p.toReal())) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(tr.label);
        }
        return sb.toString();
    }

    /** The user's selected starting XI as {@link WcPlayer}s. */
    private List<WcPlayer> userXi(WcTournament t) {
        List<WcPlayer> xi = new ArrayList<>();
        if (t.startingXiIds == null) return xi;
        Map<Integer, WcPlayer> byId = new HashMap<>();
        for (WcPlayer p : t.squad) byId.put(p.id, p);
        for (Integer id : t.startingXiIds) {
            WcPlayer p = byId.get(id);
            if (p != null) xi.add(p);
        }
        return xi;
    }

    private int rating(WcTournament t, String key) {
        Integer r = t.ratings != null ? t.ratings.get(key) : null;
        return r != null ? r : 70;
    }

    private static String displayName(String key) {
        WorldCupData.Nation n = WorldCupData.byKey(key);
        return n != null ? n.name : key;
    }

    private static int clampRating(int r) {
        return Math.max(40, Math.min(99, r));
    }
}

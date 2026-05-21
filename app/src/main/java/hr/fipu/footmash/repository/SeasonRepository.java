package hr.fipu.footmash.repository;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hr.fipu.footmash.ai.GeminiRepository;
import hr.fipu.footmash.ai.LocalSimulator;
import hr.fipu.footmash.ai.MatchSimulator;
import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.db.FixtureDao;
import hr.fipu.footmash.db.RealPlayerDao;
import hr.fipu.footmash.db.RealTeamDao;
import hr.fipu.footmash.db.StandingDao;
import hr.fipu.footmash.db.UserClubDao;
import hr.fipu.footmash.model.Fixture;
import hr.fipu.footmash.model.GoalScorer;
import hr.fipu.footmash.model.MatchResult;
import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.RealTeam;
import hr.fipu.footmash.model.SeasonStanding;
import hr.fipu.footmash.model.Trait;
import hr.fipu.footmash.model.UserClub;
import hr.fipu.footmash.model.UserSquad;
import hr.fipu.footmash.season.TraitEngine;

public class SeasonRepository {

    private final UserClubDao  userClubDao;
    private final RealTeamDao  realTeamDao;
    private final RealPlayerDao realPlayerDao;
    private final FixtureDao   fixtureDao;
    private final StandingDao  standingDao;

    public SeasonRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context.getApplicationContext());
        this.userClubDao   = db.userClubDao();
        this.realTeamDao   = db.realTeamDao();
        this.realPlayerDao = db.realPlayerDao();
        this.fixtureDao    = db.fixtureDao();
        this.standingDao   = db.standingDao();
    }

    // ─── Season initialisation ────────────────────────────────────────────────

    /** Must be called from a background thread. */
    public void startSeasonIfNeeded(int clubId) {
        if (fixtureDao.countFixtures(clubId) > 0) return;

        UserClub club = userClubDao.getClubByIdSync(clubId);
        if (club == null) return;

        List<RealTeam> allTeams = realTeamDao.getTeamsByLeagueSync(club.getLeagueId());
        
        if (club.getRealTeamSourceId() != null) {
            int sourceId = club.getRealTeamSourceId();
            for (int i = 0; i < allTeams.size(); i++) {
                if (allTeams.get(i).getId() == sourceId) {
                    allTeams.remove(i);
                    break;
                }
            }
        }
        
        int take = Math.min(19, allTeams.size());
        List<RealTeam> teams19 = new ArrayList<>(allTeams.subList(0, take));

        fixtureDao.insertAll(generateFixtures(clubId, club.getClubName(), teams19));

        List<SeasonStanding> standings = new ArrayList<>();
        SeasonStanding userRow = new SeasonStanding();
        userRow.setSeasonId(clubId);
        userRow.setTeamId(club.getId());
        userRow.setTeamName(club.getClubName());
        userRow.setUserTeam(true);
        if (club.getRealTeamSourceId() != null) {
            RealTeam sourceTeam = realTeamDao.getTeamById(club.getRealTeamSourceId());
            if (sourceTeam != null) userRow.setBadgeUrl(sourceTeam.getBadgeUrl());
        }
        standings.add(userRow);
        for (RealTeam rt : teams19) {
            SeasonStanding row = new SeasonStanding();
            row.setSeasonId(clubId);
            row.setTeamId(rt.getId());
            row.setTeamName(rt.getName());
            row.setUserTeam(false);
            row.setBadgeUrl(rt.getBadgeUrl());
            standings.add(row);
        }
        standingDao.insertAll(standings);

        // A fresh season starts with every player back at their base rating.
        realPlayerDao.resetAllFormDelta();
    }

    /**
     * Ends the current season and rolls the same club into the next one: wipes
     * last season's fixtures/results/standings, advances the year, and lets a
     * fresh fixture list be generated. The squad and formation carry over.
     * Must be called from a background thread.
     */
    public void startNextSeason(int clubId) {
        UserClub club = userClubDao.getClubByIdSync(clubId);
        if (club == null) return;

        fixtureDao.deleteMatchResultsForSeason(clubId);
        fixtureDao.deleteGoalScorersForSeason(clubId);
        fixtureDao.deleteFixturesForSeason(clubId);
        standingDao.deleteStandingsForSeason(clubId);

        club.setSeasonYear(club.getSeasonYear() + 1);
        userClubDao.updateClub(club);

        startSeasonIfNeeded(clubId);
    }

    // ─── Simulation ───────────────────────────────────────────────────────────

    /**
     * Simulates all 10 matches for the given matchday.
     * Must be called from a background thread.
     * Returns true on success.
     */
    public boolean simulateMatchday(int clubId, int matchday, String apiKey) {
        List<Fixture> fixtures = fixtureDao.getFixturesByMatchdaySync(clubId, matchday);
        if (fixtures.isEmpty()) return false;

        UserClub club = userClubDao.getClubByIdSync(clubId);
        if (club == null) return false;

        MatchSimulator.UserTeamInfo userInfo = buildUserTeamInfo(club);

        // Effective rating (avg overall + trait synergy) for every team this matchday.
        Map<String, Integer> ratings = buildRatingsMap(fixtures, club, userInfo);

        // Try Gemini with full prompt
        GeminiRepository gemini = new GeminiRepository();
        String raw = gemini.callSync(MatchSimulator.buildPrompt(fixtures, userInfo, ratings), apiKey);
        List<MatchSimulator.ParsedMatch> results = MatchSimulator.parseResponse(raw, fixtures.size());

        // Retry with simplified prompt
        if (results == null) {
            raw = gemini.callSync(MatchSimulator.buildSimplePrompt(fixtures, ratings), apiKey);
            results = MatchSimulator.parseResponse(raw, fixtures.size());
        }

        // Fall back to local simulator
        if (results == null) {
            Map<String, List<RealPlayer>> rosters = new HashMap<>();
            
            if (userInfo != null && userInfo.players != null) {
                List<RealPlayer> userPlayers = new ArrayList<>();
                for (MatchSimulator.PlayerEntry pe : userInfo.players) {
                    RealPlayer rp = new RealPlayer();
                    rp.setName(pe.name);
                    userPlayers.add(rp);
                }
                rosters.put(userInfo.name, userPlayers);
            }
            
            for (Fixture f : fixtures) {
                if (!rosters.containsKey(f.getHomeTeamName())) {
                    List<RealPlayer> p = realPlayerDao.getPlayersByTeamSync(f.getHomeTeamId());
                    rosters.put(f.getHomeTeamName(), p != null ? p : new ArrayList<>());
                }
                if (!rosters.containsKey(f.getAwayTeamName())) {
                    List<RealPlayer> p = realPlayerDao.getPlayersByTeamSync(f.getAwayTeamId());
                    rosters.put(f.getAwayTeamName(), p != null ? p : new ArrayList<>());
                }
            }
            results = LocalSimulator.simulateAll(fixtures, ratings, rosters);
        }

        saveResults(clubId, fixtures, results);
        recalculateStandings(clubId);
        applyPlayerGrowth(clubId, fixtures, results);
        return true;
    }

    // ─── Dynamic player growth ────────────────────────────────────────────────

    /**
     * Adjusts every player's {@code formDelta} after a matchday: goals (+2),
     * assists (+1), and the team result (win +1 / loss -1) for the players who
     * featured. The user's club uses its real starting XI; AI clubs use their
     * strongest XI as a proxy lineup. Cumulative form is clamped to a sane band.
     */
    private void applyPlayerGrowth(int clubId, List<Fixture> fixtures,
                                   List<MatchSimulator.ParsedMatch> results) {
        Map<Integer, Integer> growth = new HashMap<>();
        Map<Integer, List<RealPlayer>> teamCache = new HashMap<>();
        List<RealPlayer> userXi = userStartingXi(clubId);

        for (int i = 0; i < fixtures.size() && i < results.size(); i++) {
            Fixture f = fixtures.get(i);
            MatchSimulator.ParsedMatch pm = results.get(i);

            List<RealPlayer> homeXi = lineupFor(f.getHomeTeamId(), userXi, teamCache);
            List<RealPlayer> awayXi = lineupFor(f.getAwayTeamId(), userXi, teamCache);

            int homeResult = Integer.compare(pm.homeGoals, pm.awayGoals); // 1/0/-1
            for (RealPlayer p : homeXi) bump(growth, p.getId(), homeResult);
            for (RealPlayer p : awayXi) bump(growth, p.getId(), -homeResult);

            if (pm.scorers != null) {
                for (MatchSimulator.Scorer sc : pm.scorers) {
                    List<RealPlayer> lineup = "home".equals(sc.team) ? homeXi : awayXi;
                    RealPlayer scorer = matchByName(sc.name, lineup);
                    if (scorer != null) bump(growth, scorer.getId(), 2);
                    RealPlayer assister = matchByName(sc.assist, lineup);
                    if (assister != null) bump(growth, assister.getId(), 1);
                }
            }
        }

        for (Map.Entry<Integer, Integer> e : growth.entrySet()) {
            RealPlayer p = realPlayerDao.getPlayerById(e.getKey());
            if (p == null) continue;
            int updated = clampInt(p.getFormDelta() + e.getValue(), -10, 12);
            if (updated != p.getFormDelta()) {
                realPlayerDao.setFormDelta(p.getId(), updated);
            }
        }
    }

    private List<RealPlayer> userStartingXi(int clubId) {
        List<RealPlayer> xi = new ArrayList<>();
        for (UserSquad s : userClubDao.getSquadByClubSync(clubId)) {
            if (!s.isStartingXI()) continue;
            RealPlayer p = realPlayerDao.getPlayerById(s.getPlayerId());
            if (p != null) xi.add(p);
        }
        return xi;
    }

    /** The user's club (teamId 0) uses its real XI; AI clubs use a best-XI proxy. */
    private List<RealPlayer> lineupFor(int teamId, List<RealPlayer> userXi,
                                       Map<Integer, List<RealPlayer>> cache) {
        if (teamId == 0) return userXi;
        List<RealPlayer> cached = cache.get(teamId);
        if (cached != null) return cached;
        List<RealPlayer> xi = TraitEngine.bestXi(realPlayerDao.getPlayersByTeamSync(teamId));
        cache.put(teamId, xi);
        return xi;
    }

    private static void bump(Map<Integer, Integer> growth, int playerId, int amount) {
        Integer cur = growth.get(playerId);
        growth.put(playerId, (cur == null ? 0 : cur) + amount);
    }

    /** Matches a simulated scorer/assist name to a player in the given lineup. */
    private static RealPlayer matchByName(String name, List<RealPlayer> lineup) {
        if (name == null || lineup == null) return null;
        String n = name.trim().toLowerCase();
        if (n.isEmpty()) return null;
        for (RealPlayer p : lineup) {
            if (p.getName() != null && p.getName().trim().toLowerCase().equals(n)) return p;
        }
        String last = lastToken(n);
        for (RealPlayer p : lineup) {
            if (p.getName() != null && lastToken(p.getName().toLowerCase()).equals(last)) return p;
        }
        return null;
    }

    private static String lastToken(String s) {
        String[] parts = s.trim().split("\\s+");
        return parts.length == 0 ? s : parts[parts.length - 1];
    }

    private static int clampInt(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /**
     * Builds a team-name → effective-rating map for every team in the matchday.
     * The user's club uses its drafted XI; opponents use their strongest XI.
     */
    private Map<String, Integer> buildRatingsMap(List<Fixture> fixtures, UserClub club,
                                                 MatchSimulator.UserTeamInfo userInfo) {
        Map<String, Integer> ratings = new HashMap<>();
        Map<Integer, Integer> teamCache = new HashMap<>();

        if (userInfo != null) {
            ratings.put(club.getClubName(), clampRating(userInfo.effectiveOverall()));
        }
        for (Fixture f : fixtures) {
            if (!ratings.containsKey(f.getHomeTeamName())) {
                ratings.put(f.getHomeTeamName(), teamEffective(f.getHomeTeamId(), teamCache));
            }
            if (!ratings.containsKey(f.getAwayTeamName())) {
                ratings.put(f.getAwayTeamName(), teamEffective(f.getAwayTeamId(), teamCache));
            }
        }
        return ratings;
    }

    /** Effective rating of a real team, cached by team id within a matchday run. */
    private int teamEffective(int teamId, Map<Integer, Integer> cache) {
        if (teamId == 0) return 78; // user club — already keyed by name
        Integer cached = cache.get(teamId);
        if (cached != null) return cached;
        List<RealPlayer> roster = realPlayerDao.getPlayersByTeamSync(teamId);
        int eff = clampRating(TraitEngine.effectiveRating(TraitEngine.bestXi(roster)));
        cache.put(teamId, eff);
        return eff;
    }

    private static int clampRating(int r) {
        return Math.max(40, Math.min(99, r));
    }

    private MatchSimulator.UserTeamInfo buildUserTeamInfo(UserClub club) {
        List<UserSquad> squad = userClubDao.getSquadByClubSync(club.getId());
        List<MatchSimulator.PlayerEntry> players = new ArrayList<>();
        List<RealPlayer> realPlayers = new ArrayList<>();
        int sum = 0;
        for (UserSquad s : squad) {
            if (!s.isStartingXI()) continue;
            RealPlayer p = realPlayerDao.getPlayerById(s.getPlayerId());
            if (p == null) continue;
            players.add(new MatchSimulator.PlayerEntry(
                s.getPitchPosition(), p.getName(), p.getOverall(), traitLabelsOf(p)));
            realPlayers.add(p);
            sum += p.getOverall();
        }
        int avg = players.isEmpty() ? 75 : sum / players.size();
        int chemistry = computeChemistry(realPlayers);
        int synergyDelta = TraitEngine.computeSynergy(realPlayers).delta;
        return new MatchSimulator.UserTeamInfo(
            club.getClubName(), club.getFormation() != null ? club.getFormation() : "4-4-2",
            avg, chemistry, synergyDelta, players);
    }

    /** Comma-joined trait labels for a player, e.g. "Lovac na golove, Realizator". */
    private static String traitLabelsOf(RealPlayer p) {
        StringBuilder sb = new StringBuilder();
        for (Trait t : TraitEngine.deriveTraits(p)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(t.label);
        }
        return sb.toString();
    }

    /**
     * Chemistry % = share of starting XI who share nationality or original real team
     * with at least one teammate. Rewards Case B identity-preservation and penalizes
     * scattershot Case A drafting.
     */
    private static int computeChemistry(List<RealPlayer> xi) {
        int n = xi.size();
        if (n < 2) return 0;
        int linked = 0;
        for (int i = 0; i < n; i++) {
            RealPlayer a = xi.get(i);
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                RealPlayer b = xi.get(j);
                boolean sameNation = a.getNationality() != null
                    && a.getNationality().equals(b.getNationality());
                boolean sameTeam = a.getTeamId() != 0 && a.getTeamId() == b.getTeamId();
                if (sameNation || sameTeam) { linked++; break; }
            }
        }
        return (int) Math.round(100.0 * linked / n);
    }

    private void saveResults(int seasonId, List<Fixture> fixtures,
                              List<MatchSimulator.ParsedMatch> parsed) {
        for (int i = 0; i < fixtures.size(); i++) {
            Fixture f = fixtures.get(i);
            MatchSimulator.ParsedMatch pm = parsed.get(i);

            MatchResult mr = new MatchResult();
            mr.setFixtureId(f.getId());
            mr.setHomeGoals(pm.homeGoals);
            mr.setAwayGoals(pm.awayGoals);
            fixtureDao.insertMatchResult(mr);

            for (MatchSimulator.Scorer sc : pm.scorers) {
                GoalScorer gs = new GoalScorer();
                gs.setSeasonId(seasonId);
                gs.setFixtureId(f.getId());
                gs.setPlayerName(sc.name);
                gs.setTeamName("home".equals(sc.team)
                    ? f.getHomeTeamName() : f.getAwayTeamName());
                gs.setMinute(Math.max(1, Math.min(90, sc.minute)));
                gs.setUserTeamPlayer(f.isUserTeam());
                fixtureDao.insertGoalScorer(gs);
            }

            fixtureDao.markSimulated(f.getId());
        }
    }

    private void recalculateStandings(int seasonId) {
        List<SeasonStanding> rows = standingDao.getStandingsSync(seasonId);
        Map<String, SeasonStanding> map = new HashMap<>();
        for (SeasonStanding s : rows) {
            s.setPlayed(0); s.setWon(0); s.setDrawn(0); s.setLost(0);
            s.setGoalsFor(0); s.setGoalsAgainst(0); s.setPoints(0);
            map.put(s.getTeamName(), s);
        }

        for (Fixture f : fixtureDao.getSimulatedFixturesSync(seasonId)) {
            MatchResult mr = fixtureDao.getMatchResultSync(f.getId());
            if (mr == null) continue;
            applyResult(map.get(f.getHomeTeamName()), mr.getHomeGoals(), mr.getAwayGoals());
            applyResult(map.get(f.getAwayTeamName()), mr.getAwayGoals(), mr.getHomeGoals());
        }

        for (SeasonStanding s : rows) standingDao.update(s);
    }

    private static void applyResult(SeasonStanding s, int gf, int ga) {
        if (s == null) return;
        s.setPlayed(s.getPlayed() + 1);
        s.setGoalsFor(s.getGoalsFor() + gf);
        s.setGoalsAgainst(s.getGoalsAgainst() + ga);
        if (gf > ga)       { s.setWon(s.getWon() + 1);   s.setPoints(s.getPoints() + 3); }
        else if (gf == ga) { s.setDrawn(s.getDrawn() + 1); s.setPoints(s.getPoints() + 1); }
        else               { s.setLost(s.getLost() + 1); }
    }

    // ─── Fixture generation ───────────────────────────────────────────────────

    public static List<Fixture> generateFixtures(int seasonId, String userTeamName,
                                                  List<RealTeam> realTeams) {
        int n = 1 + realTeams.size();
        List<Integer> ids   = new ArrayList<>();
        List<String>  names = new ArrayList<>();
        ids.add(0);
        names.add(userTeamName);
        for (RealTeam rt : realTeams) { ids.add(rt.getId()); names.add(rt.getName()); }

        List<Integer> circle = new ArrayList<>();
        for (int i = 1; i < n; i++) circle.add(i);

        List<Fixture> firstHalf = new ArrayList<>();
        for (int round = 0; round < n - 1; round++) {
            boolean fixedHome = (round % 2 == 0);
            int c0 = circle.get(0);
            firstHalf.add(makeFixture(seasonId, round + 1,
                fixedHome ? 0 : c0, fixedHome ? c0 : 0, ids, names, true));
            for (int k = 1; k <= (n - 2) / 2; k++)
                firstHalf.add(makeFixture(seasonId, round + 1,
                    circle.get(k), circle.get(n - 1 - k), ids, names, false));
            circle.add(circle.remove(0));
        }

        List<Fixture> all = new ArrayList<>(firstHalf);
        for (Fixture f : firstHalf) {
            Fixture rev = new Fixture();
            rev.setSeasonId(seasonId);
            rev.setMatchday(f.getMatchday() + (n - 1));
            rev.setHomeTeamId(f.getAwayTeamId());
            rev.setHomeTeamName(f.getAwayTeamName());
            rev.setAwayTeamId(f.getHomeTeamId());
            rev.setAwayTeamName(f.getHomeTeamName());
            rev.setUserTeam(f.isUserTeam());
            rev.setSimulated(false);
            all.add(rev);
        }
        return all;
    }

    private static Fixture makeFixture(int seasonId, int matchday, int hi, int ai,
                                        List<Integer> ids, List<String> names, boolean user) {
        Fixture f = new Fixture();
        f.setSeasonId(seasonId); f.setMatchday(matchday);
        f.setHomeTeamId(ids.get(hi)); f.setHomeTeamName(names.get(hi));
        f.setAwayTeamId(ids.get(ai)); f.setAwayTeamName(names.get(ai));
        f.setUserTeam(user); f.setSimulated(false);
        return f;
    }
}

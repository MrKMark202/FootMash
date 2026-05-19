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
import hr.fipu.footmash.model.UserClub;
import hr.fipu.footmash.model.UserSquad;

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

        // Try Gemini with full prompt
        GeminiRepository gemini = new GeminiRepository();
        String raw = gemini.callSync(MatchSimulator.buildPrompt(fixtures, userInfo), apiKey);
        List<MatchSimulator.ParsedMatch> results = MatchSimulator.parseResponse(raw, fixtures.size());

        // Retry with simplified prompt
        if (results == null) {
            raw = gemini.callSync(MatchSimulator.buildSimplePrompt(fixtures), apiKey);
            results = MatchSimulator.parseResponse(raw, fixtures.size());
        }

        // Fall back to local simulator
        if (results == null) {
            int avg = (userInfo != null) ? userInfo.avgOverall : 78;
            String name = (userInfo != null) ? userInfo.name : "";
            
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
            results = LocalSimulator.simulateAll(fixtures, avg, name, rosters);
        }

        saveResults(clubId, fixtures, results);
        recalculateStandings(clubId);
        return true;
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
                s.getPitchPosition(), p.getName(), p.getOverall()));
            realPlayers.add(p);
            sum += p.getOverall();
        }
        int avg = players.isEmpty() ? 75 : sum / players.size();
        int chemistry = computeChemistry(realPlayers);
        return new MatchSimulator.UserTeamInfo(
            club.getClubName(), club.getFormation() != null ? club.getFormation() : "4-4-2",
            avg, chemistry, players);
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

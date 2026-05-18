package hr.fipu.footmash.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import hr.fipu.footmash.model.Fixture;

public class LocalSimulator {

    private static final Random RNG = new Random();
    private static final int DEFAULT_OVR = 78;

    public static List<MatchSimulator.ParsedMatch> simulateAll(
            List<Fixture> fixtures, int userAvgOvr, String userTeamName) {
        List<MatchSimulator.ParsedMatch> results = new ArrayList<>();
        for (Fixture f : fixtures) {
            int homeAvg = f.getHomeTeamName().equals(userTeamName) ? userAvgOvr : DEFAULT_OVR;
            int awayAvg = f.getAwayTeamName().equals(userTeamName) ? userAvgOvr : DEFAULT_OVR;
            results.add(simulate(homeAvg, awayAvg,
                f.getHomeTeamName(), f.getAwayTeamName()));
        }
        return results;
    }

    private static MatchSimulator.ParsedMatch simulate(
            int homeAvg, int awayAvg, String homeName, String awayName) {
        float diff = (homeAvg - awayAvg) / 10.0f;
        float homeWin = Math.max(0.15f, Math.min(0.75f, 0.45f + diff * 0.05f));
        float draw    = 0.25f;

        float roll = RNG.nextFloat();
        int hg, ag;
        if (roll < homeWin) {
            hg = 1 + RNG.nextInt(3);
            ag = RNG.nextInt(hg);
        } else if (roll < homeWin + draw) {
            int g = RNG.nextInt(3);
            hg = ag = g;
        } else {
            ag = 1 + RNG.nextInt(3);
            hg = RNG.nextInt(ag);
        }

        MatchSimulator.ParsedMatch m = new MatchSimulator.ParsedMatch();
        m.homeGoals = hg;
        m.awayGoals = ag;
        m.scorers   = buildScorers(hg, ag, homeName, awayName);
        return m;
    }

    private static List<MatchSimulator.Scorer> buildScorers(
            int hg, int ag, String homeName, String awayName) {
        List<MatchSimulator.Scorer> list = new ArrayList<>();
        String[] surnames = {"Silva", "Costa", "Müller", "Dupont", "García", "Rossi"};
        List<Integer> mins = randomMinutes(hg + ag);
        int idx = 0;
        for (int i = 0; i < hg; i++) {
            MatchSimulator.Scorer s = new MatchSimulator.Scorer();
            s.name   = surnames[(i * 3 + 1) % surnames.length];
            s.team   = "home";
            s.minute = mins.get(idx++);
            list.add(s);
        }
        for (int i = 0; i < ag; i++) {
            MatchSimulator.Scorer s = new MatchSimulator.Scorer();
            s.name   = surnames[(i * 3 + 4) % surnames.length];
            s.team   = "away";
            s.minute = mins.get(idx++);
            list.add(s);
        }
        return list;
    }

    private static List<Integer> randomMinutes(int count) {
        List<Integer> mins = new ArrayList<>();
        for (int i = 0; i < count; i++) mins.add(1 + RNG.nextInt(90));
        return mins;
    }
}

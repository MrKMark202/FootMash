package hr.fipu.footmash.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import java.util.Map;
import java.util.HashMap;

import hr.fipu.footmash.model.Fixture;
import hr.fipu.footmash.model.RealPlayer;

public class LocalSimulator {

    private static final Random RNG = new Random();
    private static final int DEFAULT_OVR = 78;

    public static List<MatchSimulator.ParsedMatch> simulateAll(
            List<Fixture> fixtures, int userAvgOvr, String userTeamName, Map<String, List<RealPlayer>> rosters) {
        List<MatchSimulator.ParsedMatch> results = new ArrayList<>();
        for (Fixture f : fixtures) {
            int homeAvg = f.getHomeTeamName().equals(userTeamName) ? userAvgOvr : DEFAULT_OVR;
            int awayAvg = f.getAwayTeamName().equals(userTeamName) ? userAvgOvr : DEFAULT_OVR;
            results.add(simulate(homeAvg, awayAvg,
                f.getHomeTeamName(), f.getAwayTeamName(), rosters));
        }
        return results;
    }

    private static MatchSimulator.ParsedMatch simulate(
            int homeAvg, int awayAvg, String homeName, String awayName, Map<String, List<RealPlayer>> rosters) {
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
        m.scorers   = buildScorers(hg, ag, homeName, awayName, rosters);
        return m;
    }

    private static List<MatchSimulator.Scorer> buildScorers(
            int hg, int ag, String homeName, String awayName, Map<String, List<RealPlayer>> rosters) {
        List<MatchSimulator.Scorer> list = new ArrayList<>();
        List<Integer> mins = randomMinutes(hg + ag);
        int idx = 0;
        
        List<RealPlayer> homeRoster = getRosterOrMock(rosters != null ? rosters.get(homeName) : null, homeName);
        List<RealPlayer> awayRoster = getRosterOrMock(rosters != null ? rosters.get(awayName) : null, awayName);
        
        String homeMainScorer = (hg > 1) ? getRandomScorerName(homeRoster, "HomePlayer") : null;
        String awayMainScorer = (ag > 1) ? getRandomScorerName(awayRoster, "AwayPlayer") : null;
        
        for (int i = 0; i < hg; i++) {
            MatchSimulator.Scorer s = new MatchSimulator.Scorer();
            if (homeMainScorer != null && RNG.nextFloat() < 0.55f) {
                s.name = homeMainScorer;
            } else {
                s.name = getRandomScorerName(homeRoster, "HomePlayer");
            }
            s.team   = "home";
            s.minute = mins.get(idx++);
            list.add(s);
        }
        for (int i = 0; i < ag; i++) {
            MatchSimulator.Scorer s = new MatchSimulator.Scorer();
            if (awayMainScorer != null && RNG.nextFloat() < 0.55f) {
                s.name = awayMainScorer;
            } else {
                s.name = getRandomScorerName(awayRoster, "AwayPlayer");
            }
            s.team   = "away";
            s.minute = mins.get(idx++);
            list.add(s);
        }
        return list;
    }

    private static String getRandomScorerName(List<RealPlayer> roster, String defaultName) {
        if (roster == null || roster.isEmpty()) return defaultName;
        RealPlayer rp = roster.get(RNG.nextInt(roster.size()));
        String name = rp.getName();
        if (name == null || name.trim().isEmpty()) return defaultName;
        
        String[] parts = name.trim().split("\\s+");
        if (parts.length < 2) return name;
        
        String lastName = parts[parts.length - 1];
        
        int count = 0;
        for (RealPlayer p : roster) {
            if (p.getName() != null && p.getName().endsWith(lastName)) {
                count++;
            }
        }
        
        if (count > 1) {
            return parts[0].substring(0, 1) + ". " + lastName;
        }
        return name;
    }

    private static List<RealPlayer> getRosterOrMock(List<RealPlayer> roster, String teamName) {
        if (roster != null && !roster.isEmpty()) return roster;
        List<RealPlayer> mock = new ArrayList<>();
        Random r = new Random(teamName.hashCode());
        for (int i = 0; i < 15; i++) {
            RealPlayer p = new RealPlayer();
            p.setName(generateMockName(r));
            mock.add(p);
        }
        return mock;
    }

    private static String generateMockName(Random r) {
        String[] firstNames = {
            "Luka", "Ivan", "Marko", "Mateo", "David", "Karlo", "Filip", "Josip", "Antonio", "Martin",
            "Leo", "Lucas", "Marc", "Hugo", "Daniel", "Diego", "Alex", "Thomas", "James", "Oliver",
            "Antoine", "Pierre", "Mario", "Alessandro", "Lorenzo", "Gabriel", "Arthur", "Christian"
        };
        String[] lastNames = {
            "Horvat", "Kovačević", "Babić", "Marić", "Novak", "Zubčić", "García", "Rodríguez", 
            "González", "Fernández", "López", "Martínez", "Sánchez", "Pérez", "Gomez", "Smith", 
            "Jones", "Miller", "Taylor", "Brown", "Wilson", "Müller", "Schmidt", "Fischer",
            "Dubois", "Laurent", "Rossi", "Bianchi", "Silva", "Santos", "Almeida"
        };
        return firstNames[r.nextInt(firstNames.length)] + " " + lastNames[r.nextInt(lastNames.length)];
    }

    private static List<Integer> randomMinutes(int count) {
        List<Integer> mins = new ArrayList<>();
        for (int i = 0; i < count; i++) mins.add(1 + RNG.nextInt(90));
        java.util.Collections.sort(mins);
        return mins;
    }
}

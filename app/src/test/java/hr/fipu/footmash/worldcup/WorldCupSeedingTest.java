package hr.fipu.footmash.worldcup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hr.fipu.footmash.repository.WorldCupRepository;

/** Verifies Round-of-32 qualification: 12 winners + 12 runners-up + 8 best thirds. */
public class WorldCupSeedingTest {

    private WcTournament.WcStanding standing(String key, int pts, int gf) {
        WcTournament.WcStanding s = new WcTournament.WcStanding(key);
        s.played = 3;
        s.pts = pts;
        s.gf = gf;          // ga left 0, so gd == gf
        return s;
    }

    /** 12 groups, each with a clear 1st/2nd/3rd/4th; thirds get descending strength. */
    private List<WcTournament.WcGroup> sampleGroups() {
        List<WcTournament.WcGroup> groups = new ArrayList<>();
        for (int g = 0; g < 12; g++) {
            WcTournament.WcGroup grp = new WcTournament.WcGroup();
            grp.name = String.valueOf((char) ('A' + g));
            // Strength decreases with group index so ordering is deterministic.
            grp.table.add(standing("w" + g, 9, 12 - g));   // winner
            grp.table.add(standing("r" + g, 6, 8 - g / 2)); // runner-up
            grp.table.add(standing("t" + g, 3, 12 - g));    // third (best in group A)
            grp.table.add(standing("x" + g, 0, 0));         // eliminated
            for (String k : new String[]{"w" + g, "r" + g, "t" + g, "x" + g}) {
                grp.nationKeys.add(k);
            }
            groups.add(grp);
        }
        return groups;
    }

    @Test
    public void seeds32UniqueQualifiers() {
        List<String> seeds = WorldCupRepository.computeR32Seeds(sampleGroups());
        assertEquals(32, seeds.size());
        Set<String> unique = new HashSet<>(seeds);
        assertEquals(32, unique.size());
    }

    @Test
    public void allWinnersAndRunnersQualifyPlusEightThirds() {
        List<String> seeds = WorldCupRepository.computeR32Seeds(sampleGroups());
        for (int g = 0; g < 12; g++) {
            assertTrue("winner w" + g + " must qualify", seeds.contains("w" + g));
            assertTrue("runner r" + g + " must qualify", seeds.contains("r" + g));
        }
        int thirds = 0;
        for (int g = 0; g < 12; g++) if (seeds.contains("t" + g)) thirds++;
        assertEquals(8, thirds);
        // The four weakest thirds (groups I..L) miss out.
        for (int g = 8; g < 12; g++) {
            assertTrue("weak third t" + g + " should be eliminated", !seeds.contains("t" + g));
        }
    }

    @Test
    public void strongestWinnerSeededFirst() {
        List<String> seeds = WorldCupRepository.computeR32Seeds(sampleGroups());
        assertEquals("w0", seeds.get(0));   // group A winner has the best goal diff
    }
}

package hr.fipu.footmash.ui.ailab.create;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import hr.fipu.footmash.model.CustomPlayer;
import hr.fipu.footmash.model.RealTeam;
import hr.fipu.footmash.model.Trait;

/**
 * Unit tests for the player creation wizard view model. Covers the four
 * step-validation gates, the stat allocation budget invariants, trait
 * filtering / capping, and the final buildCustomPlayer round-trip.
 */
public class PlayerCreationViewModelTest {

    @Rule public InstantTaskExecutorRule mainRule = new InstantTaskExecutorRule();

    private PlayerCreationViewModel vm;

    @Before
    public void setUp() {
        vm = new PlayerCreationViewModel();
        vm.reset();
    }

    // ─── Step 1: identity ────────────────────────────────────────────────────

    @Test
    public void step1InvalidUntilAllThreeFieldsFilled() {
        assertFalse(vm.isStep1Valid());
        vm.setFirstName("Luka");
        assertFalse(vm.isStep1Valid());
        vm.setLastName("Modrić");
        assertFalse(vm.isStep1Valid());
        vm.setNationality("Hrvatska");
        assertTrue(vm.isStep1Valid());
    }

    @Test
    public void step1IgnoresWhitespaceOnlyValues() {
        vm.setFirstName("   ");
        vm.setLastName("Modrić");
        vm.setNationality("Hrvatska");
        assertFalse("blank first name must not validate", vm.isStep1Valid());
    }

    @Test
    public void fullNameTrimsAndJoins() {
        vm.setFirstName("  Marko ");
        vm.setLastName(" Kovač  ");
        assertEquals("Marko Kovač", vm.fullName());
    }

    // ─── Step 2: stat allocation ─────────────────────────────────────────────

    @Test
    public void freshStatRowsStartAtBaseline() {
        for (int i = 0; i < PlayerCreationViewModel.STAT_COUNT; i++) {
            assertEquals(PlayerCreationViewModel.STAT_BASELINE, vm.statValueAt(i));
        }
        assertEquals(0, vm.pointsSpent());
        assertEquals(PlayerCreationViewModel.POINTS_TO_SPEND, vm.pointsRemaining());
    }

    @Test
    public void cannotSpendMoreThanBudget() {
        // Try to dump all 100 into pace (would exceed per-stat cap of 49 anyway).
        int applied = vm.trySetStatAddition(PlayerCreationViewModel.IDX_PACE, 200);
        int perStatMax = PlayerCreationViewModel.STAT_CAP - PlayerCreationViewModel.STAT_BASELINE;
        assertEquals("per-stat cap must clamp", perStatMax, applied);
        assertEquals(perStatMax, vm.pointsSpent());
    }

    @Test
    public void totalSpentNeverExceedsBudget() {
        int max = PlayerCreationViewModel.STAT_CAP - PlayerCreationViewModel.STAT_BASELINE; // 49
        vm.trySetStatAddition(PlayerCreationViewModel.IDX_PACE,      max);
        vm.trySetStatAddition(PlayerCreationViewModel.IDX_SHOOTING,  max);
        vm.trySetStatAddition(PlayerCreationViewModel.IDX_PASSING,   max);
        // We've spent 147 already — wait, that's clamped per stat. Let me redo.
        // Each call clamps to remaining budget, so:
        //  Pace = min(49, 100) = 49 → spent 49, remaining 51
        //  Shoot = min(49, 51)  = 49 → spent 98, remaining 2
        //  Pass = min(49, 2)    = 2  → spent 100, remaining 0
        assertEquals(100, vm.pointsSpent());
        assertEquals(49, vm.statValueAt(PlayerCreationViewModel.IDX_PACE)      - 50);
        assertEquals(49, vm.statValueAt(PlayerCreationViewModel.IDX_SHOOTING)  - 50);
        assertEquals( 2, vm.statValueAt(PlayerCreationViewModel.IDX_PASSING)   - 50);
    }

    @Test
    public void bumpStatRespectsBudget() {
        // Spend the whole budget on pace + shooting first.
        vm.trySetStatAddition(PlayerCreationViewModel.IDX_PACE,     49);
        vm.trySetStatAddition(PlayerCreationViewModel.IDX_SHOOTING, 49);
        vm.trySetStatAddition(PlayerCreationViewModel.IDX_PASSING,   2);
        assertEquals(100, vm.pointsSpent());

        // Pumping passing further should no-op (budget full).
        vm.bumpStat(PlayerCreationViewModel.IDX_PASSING, +5);
        assertEquals(100, vm.pointsSpent());

        // Decrementing should free budget that other stats can claim.
        vm.bumpStat(PlayerCreationViewModel.IDX_PASSING, -2);
        assertEquals(98, vm.pointsSpent());
        vm.bumpStat(PlayerCreationViewModel.IDX_DRIBBLING, +2);
        assertEquals(100, vm.pointsSpent());
    }

    @Test
    public void overallReflectsSpentPoints() {
        // Empty allocation → all 50s → OVR 50
        assertEquals(50, vm.currentOverall());

        // Spend 60 points across 6 stats → average 60 → OVR 60.
        vm.trySetStatAddition(PlayerCreationViewModel.IDX_PACE,      10);
        vm.trySetStatAddition(PlayerCreationViewModel.IDX_SHOOTING,  10);
        vm.trySetStatAddition(PlayerCreationViewModel.IDX_PASSING,   10);
        vm.trySetStatAddition(PlayerCreationViewModel.IDX_DRIBBLING, 10);
        vm.trySetStatAddition(PlayerCreationViewModel.IDX_DEFENDING, 10);
        vm.trySetStatAddition(PlayerCreationViewModel.IDX_PHYSICAL,  10);
        assertEquals(60, vm.currentOverall());
    }

    @Test
    public void step2InvalidUntilBudgetSpentAndPositionPicked() {
        assertFalse(vm.isStep2Valid());
        vm.setPosition("ST");
        assertFalse("position alone is not enough", vm.isStep2Valid());

        // Spend everything.
        vm.trySetStatAddition(PlayerCreationViewModel.IDX_PACE,     49);
        vm.trySetStatAddition(PlayerCreationViewModel.IDX_SHOOTING, 49);
        vm.trySetStatAddition(PlayerCreationViewModel.IDX_PASSING,   2);
        assertTrue(vm.isStep2Valid());
    }

    // ─── Step 3: trait filtering / capping ───────────────────────────────────

    @Test
    public void eligibleTraitsFilterByPositionGroup() {
        vm.setPosition("ST"); // FW group
        for (Trait t : vm.eligibleTraits()) {
            assertEquals("only FW traits eligible for ST", "FW", t.group);
        }
        // FW traits in the enum: 5 of them.
        assertEquals(5, vm.eligibleTraits().size());
    }

    @Test
    public void toggleTraitAddsThenRemoves() {
        vm.setPosition("ST");
        Trait poacher = Trait.GOAL_POACHER;
        vm.toggleTrait(poacher);
        assertTrue(vm.isTraitSelected(poacher));
        vm.toggleTrait(poacher);
        assertFalse(vm.isTraitSelected(poacher));
    }

    @Test
    public void capsAtMaxTraits() {
        vm.setPosition("ST");
        vm.toggleTrait(Trait.GOAL_POACHER);
        vm.toggleTrait(Trait.CLINICAL_FINISHER);
        vm.toggleTrait(Trait.SPEED_MERCHANT);
        assertEquals(3, vm.traitsSelectedCount());

        // Fourth pick must be silently dropped.
        vm.toggleTrait(Trait.FALSE_NINE);
        assertFalse(vm.isTraitSelected(Trait.FALSE_NINE));
        assertEquals(3, vm.traitsSelectedCount());
    }

    @Test
    public void rejectsOutOfGroupTraits() {
        vm.setPosition("ST"); // FW group
        // PLAYMAKER is a MF trait.
        vm.toggleTrait(Trait.PLAYMAKER);
        assertFalse(vm.isTraitSelected(Trait.PLAYMAKER));
    }

    @Test
    public void step3RequiresAtLeastOneTrait() {
        vm.setPosition("ST");
        assertFalse(vm.isStep3Valid());
        vm.toggleTrait(Trait.GOAL_POACHER);
        assertTrue(vm.isStep3Valid());
    }

    // ─── Step 4: club selection ──────────────────────────────────────────────

    @Test
    public void leagueChangeWipesOffersAndSelection() {
        // Pre-populate as if step 4 had run once.
        vm.setSelectedLeague(177, "Premier League");
        RealTeam club = new RealTeam();
        club.setId(1);
        club.setName("Foo FC");
        vm.selectClub(club);
        assertNotNull(vm.getSelectedClub().getValue());

        // Switching league wipes both.
        vm.setSelectedLeague(302, "La Liga");
        assertEquals(0, vm.getCurrentOffers().getValue().size());
        assertEquals(null, vm.getSelectedClub().getValue());
        assertFalse(vm.isStep4Valid());
    }

    // ─── Full build round-trip ───────────────────────────────────────────────

    @Test
    public void buildCustomPlayerProducesFullyPopulatedRow() {
        fillAllSteps();
        CustomPlayer p = vm.buildCustomPlayer();

        assertEquals("Marko", p.getFirstName());
        assertEquals("Kovač", p.getLastName());
        assertEquals("Hrvatska", p.getNationality());
        assertEquals("ST", p.getPosition());
        assertEquals(PlayerCreationViewModel.DEFAULT_AGE, p.getAge());
        assertEquals(PlayerCreationViewModel.DEFAULT_SEASON, p.getTargetSeason());

        assertEquals(50 + 49, p.getPace());
        assertEquals(50 + 49, p.getShooting());
        assertEquals(50 +  2, p.getPassing());
        assertEquals(50, p.getDribbling());
        assertEquals(50, p.getDefending());
        assertEquals(50, p.getPhysical());

        assertEquals(42, p.getTargetTeamId());
        assertEquals("Test FC", p.getTargetTeamName());
        assertEquals(177, p.getTargetLeagueId());
        assertEquals("GOAL_POACHER", p.getTraits());
    }

    @Test(expected = IllegalStateException.class)
    public void buildCustomPlayerThrowsIfWizardIncomplete() {
        // Only step 1 done.
        vm.setFirstName("X");
        vm.setLastName("Y");
        vm.setNationality("Z");
        vm.buildCustomPlayer();
    }

    private void fillAllSteps() {
        // Step 1
        vm.setFirstName("Marko");
        vm.setLastName("Kovač");
        vm.setNationality("Hrvatska");
        // Step 2
        vm.setPosition("ST");
        vm.trySetStatAddition(PlayerCreationViewModel.IDX_PACE,     49);
        vm.trySetStatAddition(PlayerCreationViewModel.IDX_SHOOTING, 49);
        vm.trySetStatAddition(PlayerCreationViewModel.IDX_PASSING,   2);
        // Step 3
        vm.toggleTrait(Trait.GOAL_POACHER);
        // Step 4
        vm.setSelectedLeague(177, "Premier League");
        RealTeam club = new RealTeam();
        club.setId(42);
        club.setName("Test FC");
        club.setBadgeUrl("https://example.com/badge.png");
        vm.selectClub(club);
    }
}

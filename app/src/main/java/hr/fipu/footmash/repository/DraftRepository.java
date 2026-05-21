package hr.fipu.footmash.repository;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hr.fipu.footmash.db.AppDatabase;
import hr.fipu.footmash.model.FormationSlot;
import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.UserSquad;
import hr.fipu.footmash.season.FormationCatalog;

public class DraftRepository {

    private final AppDatabase db;

    public DraftRepository(AppDatabase db) {
        this.db = db;
    }

    /**
     * Pre-seeds an existing-club draft: pulls the real team's roster, best-fits the 11
     * strongest players into the formation slots by position group, and inserts them
     * as starting-XI rows in user_squad. Caller is responsible for setting the club's
     * budget to HALF_BUDGET separately.
     */
    public void seedExistingClub(int clubId, int realTeamId, String formation) {
        List<RealPlayer> roster = db.realPlayerDao().getPlayersByTeamSync(realTeamId);
        if (roster == null || roster.isEmpty()) return;
        Collections.sort(roster, (a, b) -> Integer.compare(b.getOverall(), a.getOverall()));

        List<FormationSlot> slots = FormationCatalog.get(formation);
        Set<Integer> used = new HashSet<>();

        for (FormationSlot slot : slots) {
            for (RealPlayer p : roster) {
                if (used.contains(p.getId())) continue;
                if (!FormationCatalog.matchesPosGroup(p.getPosition(), slot.posGroup)) continue;

                UserSquad sq = new UserSquad();
                sq.setClubId(clubId);
                sq.setPlayerId(p.getId());
                sq.setStartingXI(true);
                sq.setPitchPosition(slot.key);
                db.userClubDao().insertSquadPlayer(sq);
                used.add(p.getId());
                break;
            }
        }

        // Everyone else from the real roster joins the bench, so the whole
        // squad is available for substitutions and re-drafting.
        for (RealPlayer p : roster) {
            if (used.contains(p.getId())) continue;
            UserSquad sub = new UserSquad();
            sub.setClubId(clubId);
            sub.setPlayerId(p.getId());
            sub.setStartingXI(false);
            sub.setPitchPosition(null);
            db.userClubDao().insertSquadPlayer(sub);
        }
    }
}

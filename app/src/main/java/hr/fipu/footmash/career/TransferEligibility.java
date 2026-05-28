package hr.fipu.footmash.career;

/**
 * Pure helper for the "do offers open this turn?" decision.
 *
 * <p>The user gets their first offer cluster after 2 seasons at a club.
 * If they decline (stay), the window stays shut until 2 more seasons pass —
 * so declining never traps them out of the regular simulate loop.
 *
 * <p>Lives in its own class (rather than baked into the ViewModel) so the
 * rule is testable end-to-end without Android dependencies.
 */
public final class TransferEligibility {

    /** Seasons-at-club threshold for the first offer cluster. */
    public static final int FIRST_WINDOW_AT = 2;
    /** Wait this many seasons after a decline before offering again. */
    public static final int COOLDOWN_SEASONS = 2;

    private TransferEligibility() {}

    /**
     * @param seasonsAtClub       seasons logged for this player at the current club
     * @param transferDismissedAt the seasons-at-club value recorded when the user
     *                            last picked "Ostani"; -1 means "no dismissal yet"
     * @return {@code true} when the career hub should surface the
     *         "Prelazni rok" CTA right now.
     */
    public static boolean isEligible(int seasonsAtClub, int transferDismissedAt) {
        if (seasonsAtClub < FIRST_WINDOW_AT) return false;
        if (transferDismissedAt < 0) return true;
        return seasonsAtClub - transferDismissedAt >= COOLDOWN_SEASONS;
    }
}

package hr.fipu.footmash.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The outcome of evaluating trait combinations across a starting XI.
 *
 * <ul>
 *   <li>{@link #rating} — a 0–100 display score (≈50 is a neutral squad).</li>
 *   <li>{@link #delta}  — points added to the team's effective rating in simulation
 *       (clamped to a small range so synergy tilts matches rather than deciding them).</li>
 *   <li>{@link #positives} / {@link #negatives} — human-readable combo descriptions
 *       for the synergy breakdown dialog.</li>
 * </ul>
 */
public class SynergyResult {

    public final int rating;
    public final int delta;
    public final List<String> positives;
    public final List<String> negatives;

    public SynergyResult(int rating, int delta, List<String> positives, List<String> negatives) {
        this.rating = rating;
        this.delta = delta;
        this.positives = positives;
        this.negatives = negatives;
    }

    /** A neutral result for an empty or single-player lineup. */
    public static SynergyResult empty() {
        return new SynergyResult(50, 0, new ArrayList<>(), new ArrayList<>());
    }
}

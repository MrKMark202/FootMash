package hr.fipu.footmash.ui.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.SparseIntArray;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * Maps a seed team_id to a bundled drawable resource ID. To plug in a real crest:
 *
 *   1. Drop a PNG / WebP / SVG-converted vector into res/drawable/, e.g.
 *      res/drawable/badge_team_85.png            (Manchester City, team_id 85)
 *   2. Add one line to the static init block below:
 *      BY_TEAM_ID.put(85, R.drawable.badge_team_85);
 *   3. Rebuild — adapters that call {@link #badgeFor(Context, int, String)} will
 *      use the bundled drawable when present and fall back to a generated
 *      initials badge otherwise.
 *
 * Keep this class as the single source of truth for badge lookups so adding /
 * removing crests is a one-line change.
 */
public final class TeamBadges {

    private static final SparseIntArray BY_TEAM_ID = new SparseIntArray();

    static {
        // ---- Premier League ---------------------------------------------------
        // BY_TEAM_ID.put(85,  R.drawable.badge_team_85);   // Manchester City
        // BY_TEAM_ID.put(62,  R.drawable.badge_team_62);   // Arsenal
        // BY_TEAM_ID.put(32,  R.drawable.badge_team_32);   // Liverpool
        // BY_TEAM_ID.put(38,  R.drawable.badge_team_38);   // Chelsea
        // BY_TEAM_ID.put(33,  R.drawable.badge_team_33);   // Manchester United
        // BY_TEAM_ID.put(73,  R.drawable.badge_team_73);   // Tottenham Hotspur
        // BY_TEAM_ID.put(51,  R.drawable.badge_team_51);   // Newcastle United
        // BY_TEAM_ID.put(66,  R.drawable.badge_team_66);   // Aston Villa
        // ... add the rest as you supply assets

        // ---- La Liga ----------------------------------------------------------
        // BY_TEAM_ID.put(83,  R.drawable.badge_team_83);   // Real Madrid
        // BY_TEAM_ID.put(84,  R.drawable.badge_team_84);   // Barcelona
        // ...
    }

    private TeamBadges() {}

    /** @return a bundled drawable resource id for the team, or 0 if none is registered. */
    @DrawableRes
    public static int resFor(int teamId) {
        return BY_TEAM_ID.get(teamId, 0);
    }

    /**
     * Convenience: returns the bundled crest if one is registered for {@code teamId},
     * otherwise returns a generated {@link InitialsBadgeDrawable} built from {@code fallbackName}.
     */
    @NonNull
    public static Drawable badgeFor(@NonNull Context context, int teamId, @Nullable String fallbackName) {
        int res = resFor(teamId);
        if (res != 0) {
            Drawable d = ContextCompat.getDrawable(context, res);
            if (d != null) return d;
        }
        return new InitialsBadgeDrawable(fallbackName);
    }
}

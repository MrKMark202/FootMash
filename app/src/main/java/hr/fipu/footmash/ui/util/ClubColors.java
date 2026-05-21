package hr.fipu.footmash.ui.util;

import android.content.res.ColorStateList;
import android.util.SparseArray;

import androidx.core.graphics.ColorUtils;

import com.google.android.material.button.MaterialButton;

/**
 * Per-club colour identity for Season Mode. When the user inherits a real club
 * (see {@code UserClub.realTeamSourceId}), the season screens are re-skinned with
 * that club's colours instead of the default app accent.
 *
 * Keyed by seed {@code team_id} — the immutable primary key of {@code real_teams}.
 * Unknown ids (and self-founded clubs, which have no source team) fall back to
 * {@link #DEFAULT}.
 *
 * Each {@link Theme} carries three colours:
 * <ul>
 *   <li>{@code primary}   — the club's vivid brand colour; used for buttons and
 *       headline highlights. Deliberately chosen to stay visible on the dark
 *       app background, so a few dark kits are represented by their brighter shade.</li>
 *   <li>{@code onPrimary} — text/icon colour with contrast against {@code primary}.</li>
 *   <li>{@code accent}    — the club's secondary colour; used for strokes and the
 *       translucent user-row highlight ({@link Theme#rowTint()}).</li>
 * </ul>
 */
public final class ClubColors {

    private ClubColors() {}

    public static final class Theme {
        public final int primary;
        public final int onPrimary;
        public final int accent;

        Theme(int primary, int onPrimary, int accent) {
            this.primary = primary;
            this.onPrimary = onPrimary;
            this.accent = accent;
        }

        /** Translucent accent for highlighting the user's row in the league table. */
        public int rowTint() {
            return ColorUtils.setAlphaComponent(accent, 0x40);
        }
    }

    /** App default (Material Blue) — used for self-founded clubs and unknown ids. */
    public static final Theme DEFAULT = new Theme(0xFF2962FF, 0xFFFFFFFF, 0xFF2962FF);

    private static final int W = 0xFFFFFFFF; // white  (onPrimary for dark kits)
    private static final int K = 0xFF0A0A0A; // near-black (onPrimary for light kits)

    private static final SparseArray<Theme> BY_TEAM_ID = new SparseArray<>();

    private static void t(int teamId, int primary, int onPrimary, int accent) {
        BY_TEAM_ID.put(teamId, new Theme(primary, onPrimary, accent));
    }

    static {
        // ─── Premier League ────────────────────────────────────────────────────
        t(85,  0xFF6CABDD, K, 0xFF1C2C5B); // Manchester City
        t(62,  0xFFEF0107, W, 0xFFFFD700); // Arsenal
        t(32,  0xFFC8102E, W, 0xFF00B2A9); // Liverpool
        t(38,  0xFF2A6DE0, W, 0xFFDBA111); // Chelsea
        t(33,  0xFFDA291C, W, 0xFFFBE122); // Manchester United
        t(73,  W,          K, 0xFF132257); // Tottenham Hotspur
        t(51,  W,          K, 0xFF241F20); // Newcastle United
        t(66,  0xFF95BFE5, K, 0xFF670E36); // Aston Villa
        t(37,  0xFFB5294A, W, 0xFF1BB1E7); // West Ham United
        t(78,  0xFF0057B8, W, W);          // Brighton & Hove Albion
        t(56,  0xFFFDB913, K, 0xFF231F20); // Wolverhampton Wanderers
        t(31,  0xFF2A52BE, W, W);          // Everton
        t(53,  0xFF2A5BB5, W, 0xFFC4122E); // Crystal Palace
        t(134, 0xFFE30613, W, W);          // Brentford
        t(40,  W,          K, 0xFFCC0000); // Fulham
        t(70,  0xFFDA291C, W, 0xFF111111); // AFC Bournemouth
        t(93,  0xFFDD0000, W, W);          // Nottingham Forest
        t(96,  W,          K, 0xFFFFCD00); // Leeds United
        t(97,  0xFF97D2EA, K, 0xFF6C1D45); // Burnley
        t(98,  0xFFEB172B, W, W);          // Sunderland

        // ─── La Liga ───────────────────────────────────────────────────────────
        // Note: La Liga's FC Barcelona uses team_id 73, which already belongs to
        // Tottenham (Premier League) in real_teams — so no Barcelona entry is added
        // here; id 73 keeps the Tottenham theme.
        t(83,  W,          K, 0xFFFEBE10); // Real Madrid
        t(116, 0xFFCB3524, W, 0xFF262E62); // Atletico Madrid
        t(126, 0xFFEE2523, W, W);          // Athletic Club
        t(120, 0xFF0067B1, W, W);          // Real Sociedad
        t(101, 0xFFFFD000, K, 0xFF005187); // Villarreal CF
        t(123, 0xFF00954C, W, W);          // Real Betis
        t(124, W,          K, 0xFFF18E00); // Valencia CF
        t(260, 0xFFFFE400, K, 0xFF004F9E); // UD Las Palmas
        t(135, 0xFFD81E05, W, 0xFF0A346F); // CA Osasuna
        t(251, W,          K, 0xFFE53027); // Rayo Vallecano
        t(138, 0xFF6FB7E8, K, 0xFFE5231B); // RC Celta Vigo
        t(139, 0xFF005999, W, W);          // Getafe CF
        t(119, W,          K, 0xFFD81A33); // Sevilla FC
        t(253, 0xFFD10A11, W, W);          // Girona FC
        t(144, 0xFF004B9E, W, W);          // Deportivo Alaves
        t(143, 0xFFE30613, W, 0xFF111111); // RCD Mallorca
        t(256, 0xFF1E63B8, W, W);          // CD Leganes
        t(259, 0xFFA8439A, W, W);          // Real Valladolid
        t(153, 0xFF0E4DA4, W, 0xFF8B1A3A); // Levante UD

        // ─── Bundesliga ────────────────────────────────────────────────────────
        t(701, 0xFFDC052D, W, W);          // Bayern Munich
        t(702, 0xFFE32219, W, 0xFF111111); // Bayer Leverkusen
        t(703, 0xFFFDE100, K, 0xFF111111); // Borussia Dortmund
        t(704, W,          K, 0xFFDD0741); // RB Leipzig
        t(705, 0xFFE1000F, W, 0xFF111111); // Eintracht Frankfurt
        t(706, W,          K, 0xFFE32219); // VfB Stuttgart
        t(707, W,          K, 0xFF00A94F); // Borussia Monchengladbach
        t(708, 0xFF65B32E, K, W);          // VfL Wolfsburg
        t(709, 0xFFE32219, W, W);          // SC Freiburg
        t(710, 0xFFD14843, W, 0xFF46714D); // FC Augsburg
        t(711, 0xFF1C63B7, W, W);          // TSG Hoffenheim
        t(712, 0xFF1D9053, W, W);          // Werder Bremen
        t(713, 0xFFEB1923, W, 0xFFFFE600); // Union Berlin
        t(714, 0xFFE2001A, W, 0xFF003F87); // 1. FC Heidenheim
        t(715, 0xFFED1C24, W, W);          // Mainz 05
        t(716, W,          K, 0xFF6B3F25); // FC St. Pauli
        t(717, W,          K, 0xFFED1C24); // 1. FC Koln
        t(718, 0xFF1E5AA8, W, W);          // Hamburger SV
        t(719, 0xFF1E63C0, W, W);          // Schalke 04
        t(720, 0xFF005CA9, W, W);          // Hertha BSC

        // ─── Serie A ───────────────────────────────────────────────────────────
        t(901, 0xFF199ED8, W, W);          // Napoli
        t(902, 0xFF1378C5, W, 0xFF111111); // Inter
        t(903, 0xFF1E71B8, W, 0xFF111111); // Atalanta
        t(904, W,          K, 0xFF111111); // Juventus
        t(905, 0xFFB8293C, W, 0xFFF0BC42); // AS Roma
        t(906, 0xFF7B45A8, W, W);          // Fiorentina
        t(907, 0xFF6FC6E8, K, W);          // Lazio
        t(908, 0xFFFB090B, W, 0xFF111111); // AC Milan
        t(909, 0xFFC73441, W, 0xFF1A2D5A); // Bologna
        t(910, 0xFF0B5BA8, W, W);          // Como
        t(911, 0xFFAE2A3D, W, W);          // Torino
        t(912, W,          K, 0xFF111111); // Udinese
        t(913, 0xFFB5293A, W, 0xFF002E5B); // Genoa
        t(914, 0xFFFFE400, K, 0xFF0A2F66); // Hellas Verona
        t(915, 0xFFC73058, W, 0xFF1B2A4A); // Cagliari
        t(916, 0xFFFFD400, K, 0xFF1A4F9E); // Parma
        t(917, 0xFFFFE000, K, 0xFFE2001A); // Lecce
        t(918, 0xFF1B5FA8, W, 0xFF111111); // Pisa
        t(919, 0xFFC73142, W, 0xFF9AA0A6); // Cremonese
        t(920, 0xFF00A551, W, 0xFF111111); // Sassuolo

        // ─── Ligue 1 ───────────────────────────────────────────────────────────
        t(1101, 0xFF1E6FA8, W, 0xFFDA1A32); // Paris Saint-Germain
        t(1102, 0xFF2FAEE0, W, W);          // Marseille
        t(1103, 0xFFE51B22, W, W);          // Monaco
        t(1104, 0xFFE01E13, W, 0xFF0A2B5C); // Lille
        t(1105, 0xFFE4022D, W, 0xFF111111); // Nice
        t(1106, W,          K, 0xFFDA1A32); // Lyon
        t(1107, 0xFFFFE000, K, 0xFFE2001A); // Lens
        t(1108, 0xFF009FE3, W, W);          // Strasbourg
        t(1109, 0xFFE23A2E, W, 0xFF111111); // Rennes
        t(1110, 0xFF8A45A8, W, W);          // Toulouse
        t(1111, 0xFFE2001A, W, W);          // Brest
        t(1112, W,          K, 0xFF0A4FA0); // Auxerre
        t(1113, 0xFFFFD200, K, 0xFF008B47); // Nantes
        t(1114, 0xFF1C9CD8, W, 0xFF0A2A5C); // Le Havre
        t(1115, W,          K, 0xFF111111); // Angers
        t(1116, 0xFFA3293F, W, W);          // Metz
        t(1117, 0xFFF36F21, K, 0xFF111111); // Lorient
        t(1118, 0xFF1E4FA0, W, 0xFFDA1A32); // Paris FC
        t(1119, 0xFF008B47, W, W);          // Saint-Etienne
        t(1120, 0xFF2A52B5, W, 0xFFF36F21); // Montpellier
    }

    /** Theme for the given source team id; {@link #DEFAULT} for null/unknown ids. */
    public static Theme of(Integer teamId) {
        if (teamId == null) return DEFAULT;
        Theme t = BY_TEAM_ID.get(teamId);
        return t != null ? t : DEFAULT;
    }

    /** Tints a button with the theme's primary colour and contrasting text. */
    public static void styleButton(MaterialButton button, Theme theme) {
        if (button == null || theme == null) return;
        button.setBackgroundTintList(ColorStateList.valueOf(theme.primary));
        button.setTextColor(theme.onPrimary);
    }
}

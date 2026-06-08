package hr.fipu.footmash.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.model.RealPlayer;
import hr.fipu.footmash.model.RealTeam;

public class SeedLoader {

    private static final String TAG = "SeedLoader";
    private static final String PREFS_NAME = "footmash_prefs";

    /**
     * Bump this key whenever the bundled JSON files change so existing installs
     * pick up the new data on next launch. Old rows are wiped before reload so
     * stale players from a previous season don't linger.
     */
    private static final String KEY_SEED_LOADED = "seed_loaded_v7_spurs_id_fix";

    private static final String[] SEED_FILES = {
        "data/premierleague.json",
        "data/laliga.json",
        "data/bundesliga.json",
        "data/seriea.json",
        "data/ligue1.json"
    };

    /**
     * Clubs dropped from the bundled JSON so the seeded set matches the 26/27
     * logo pack (Bundesliga & Ligue 1 also shrink to their real 18-team size).
     * The replacement promoted sides are injected from {@link ExtraClubs}.
     */
    private static final java.util.Set<String> REMOVED_CLUBS = new java.util.HashSet<>(
        java.util.Arrays.asList(
            "West Ham United", "Wolverhampton Wanderers", "Burnley",   // Premier League
            "UD Las Palmas", "CD Leganes", "Real Valladolid",          // La Liga
            "Schalke 04", "Hertha BSC",                                // Bundesliga (→18)
            "Saint-Etienne", "Montpellier"));                          // Ligue 1 (→18)

    /** ID cursors for injected clubs/players — well above the bundled ranges. */
    private static int nextExtraTeamId = 9001;
    private static int nextExtraPlayerId = 90001;

    public static void loadIfNeeded(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_SEED_LOADED, false)) {
            return;
        }

        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                Gson gson = new Gson();

                // Wipe any rows from a prior seed version before reloading.
                db.realPlayerDao().deleteAll();
                db.realTeamDao().deleteAll();

                nextExtraTeamId = 9001;
                nextExtraPlayerId = 90001;

                for (String fileName : SEED_FILES) {
                    loadFile(context, db, gson, fileName);
                }

                prefs.edit().putBoolean(KEY_SEED_LOADED, true).apply();
                Log.i(TAG, "Seed data loaded successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to load seed data", e);
            }
        }).start();
    }

    private static void loadFile(Context context, AppDatabase db, Gson gson, String fileName)
            throws IOException {
        InputStream is = context.getAssets().open(fileName);
        SeedLeagueData data = gson.fromJson(
            new InputStreamReader(is, StandardCharsets.UTF_8),
            SeedLeagueData.class
        );
        is.close();

        if (data == null || data.teams == null) return;

        int leagueId = data.league.id;
        String leagueName = data.league.name;
        String country = data.league.country;
        String iconFolder = LogoAssets.folderForLeague(leagueId);

        List<RealTeam> teams = new ArrayList<>();
        List<RealPlayer> players = new ArrayList<>();

        for (SeedTeamData teamData : data.teams) {
            // Drop clubs that aren't part of the 26/27 set.
            if (REMOVED_CLUBS.contains(teamData.name)) continue;

            RealTeam team = new RealTeam();
            team.setId(teamData.teamId);
            team.setName(teamData.name);

            // Prefer a bundled local logo; fall back to the JSON/remote badge.
            String bUrl = teamData.badgeUrl;
            String localRel = LogoAssets.resolveClub(context, iconFolder, teamData.name);
            if (localRel != null) {
                bUrl = LogoAssets.assetUri(localRel);
            } else if (bUrl == null || bUrl.isEmpty()) {
                String kebabName = teamData.name.toLowerCase()
                    .replace(" & ", "-")
                    .replace("&", "")
                    .replace(" ", "-")
                    .replace(".", "");
                bUrl = "https://apiv2.allsportsapi.com/logo/" + teamData.teamId + "_" + kebabName + ".jpg";
            }
            team.setBadgeUrl(bUrl);
            team.setLeagueId(leagueId);
            team.setLeagueName(leagueName);
            team.setCountry(country);
            teams.add(team);

            if (teamData.players == null) continue;
            for (SeedPlayerData p : teamData.players) {
                RealPlayer player = new RealPlayer();
                player.setId(p.playerId);
                player.setName(p.name);
                player.setPosition(p.position);
                player.setNationality(p.nationality);
                player.setAge(p.age);
                player.setPace(p.pace);
                player.setShooting(p.shooting);
                player.setPassing(p.passing);
                player.setDribbling(p.dribbling);
                player.setDefending(p.defending);
                player.setPhysical(p.physical);
                player.setOverall(p.overall);
                player.setTeamId(teamData.teamId);
                player.setTeamName(teamData.name);
                player.setLeagueId(leagueId);
                player.setLeagueName(leagueName);
                players.add(player);
            }
        }

        // Inject the promoted clubs that replace the removed ones for this league.
        for (ExtraClubs.XClub xc : ExtraClubs.forLeague(leagueId)) {
            int tid = nextExtraTeamId++;
            RealTeam team = new RealTeam();
            team.setId(tid);
            team.setName(xc.name);
            String rel = LogoAssets.resolveClub(context, iconFolder, xc.name);
            team.setBadgeUrl(rel != null ? LogoAssets.assetUri(rel) : "");
            team.setLeagueId(leagueId);
            team.setLeagueName(leagueName);
            team.setCountry(country);
            teams.add(team);

            for (ExtraClubs.XPlayer xp : xc.players) {
                RealPlayer player = new RealPlayer();
                player.setId(nextExtraPlayerId++);
                player.setName(xp.name);
                player.setPosition(xp.position);
                player.setNationality(xp.nationality);
                player.setAge(xp.age);
                player.setPace(xp.pace);
                player.setShooting(xp.shooting);
                player.setPassing(xp.passing);
                player.setDribbling(xp.dribbling);
                player.setDefending(xp.defending);
                player.setPhysical(xp.physical);
                player.setOverall(xp.overall);
                player.setTeamId(tid);
                player.setTeamName(xc.name);
                player.setLeagueId(leagueId);
                player.setLeagueName(leagueName);
                players.add(player);
            }
        }

        db.realTeamDao().insertAll(teams);
        db.realPlayerDao().insertAll(players);
        Log.i(TAG, "Loaded " + teams.size() + " teams, " + players.size() + " players from " + fileName);
    }

    // --- Gson POJO classes ---

    static class SeedLeagueData {
        @SerializedName("league") SeedLeagueInfo league;
        @SerializedName("teams") List<SeedTeamData> teams;
    }

    static class SeedLeagueInfo {
        @SerializedName("id") int id;
        @SerializedName("name") String name;
        @SerializedName("country") String country;
        @SerializedName("season") int season;
    }

    static class SeedTeamData {
        @SerializedName("team_id") int teamId;
        @SerializedName("name") String name;
        @SerializedName("badge_url") String badgeUrl;
        @SerializedName("players") List<SeedPlayerData> players;
    }

    static class SeedPlayerData {
        @SerializedName("player_id") int playerId;
        @SerializedName("name") String name;
        @SerializedName("position") String position;
        @SerializedName("nationality") String nationality;
        @SerializedName("age") int age;
        @SerializedName("pace") int pace;
        @SerializedName("shooting") int shooting;
        @SerializedName("passing") int passing;
        @SerializedName("dribbling") int dribbling;
        @SerializedName("defending") int defending;
        @SerializedName("physical") int physical;
        @SerializedName("overall") int overall;
    }
}

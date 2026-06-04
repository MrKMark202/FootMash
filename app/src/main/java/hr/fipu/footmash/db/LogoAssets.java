package hr.fipu.footmash.db;

import android.content.Context;

import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves a club name to a bundled local logo under {@code assets/icons/<folder>/}.
 * Matching is accent- and punctuation-insensitive, with an alias table for the
 * handful of clubs whose display name doesn't normalise to their logo's filename
 * slug (e.g. "Bayern Munich" → "bayern-munchen").
 */
public final class LogoAssets {

    private static final String SUFFIX = ".football-logos.cc.png";

    /** Maps each seeded league id to its icon sub-folder under assets/icons/. */
    public static String folderForLeague(int leagueId) {
        switch (leagueId) {
            case 177: return "icons/Prem";
            case 302: return "icons/Laliga";
            case 78:  return "icons/bundes";
            case 207: return "icons/seria a";
            case 168: return "icons/lige 1";
            default:  return null;
        }
    }

    /** normalised team name → logo filename slug (without the suffix). */
    private static final Map<String, String> ALIAS = new HashMap<>();
    static {
        ALIAS.put("bayernmunich", "bayern-munchen");
        ALIAS.put("unionberlin", "union-berlin");
        ALIAS.put("1fcunionberlin", "union-berlin");
        ALIAS.put("fcheidenheim", "fc-heidenheim");
        ALIAS.put("1fcheidenheim", "fc-heidenheim");
        ALIAS.put("heidenheim", "fc-heidenheim");
        ALIAS.put("fckoln", "koln");
        ALIAS.put("1fckoln", "koln");
        ALIAS.put("mainz05", "mainz-05");
        ALIAS.put("1fsvmainz05", "mainz-05");
        ALIAS.put("fsvmainz05", "mainz-05");
        ALIAS.put("rbleipzig", "rb-leipzig");
        ALIAS.put("fcstpauli", "st-pauli");
        ALIAS.put("stpauli", "st-pauli");
        ALIAS.put("tsghoffenheim", "hoffenheim");
        ALIAS.put("1899hoffenheim", "hoffenheim");
        ALIAS.put("scfreiburg", "freiburg");
        ALIAS.put("vflwolfsburg", "wolfsburg");
        ALIAS.put("asmonaco", "as-monaco");
        ALIAS.put("rclens", "rc-lens");
        ALIAS.put("rcstrasbourg", "rc-strasbourg-alsace");
        ALIAS.put("rcstrasbourgalsace", "rc-strasbourg-alsace");
        ALIAS.put("stadebrestois29", "brest");
        ALIAS.put("stadebrestois", "brest");
        ALIAS.put("lehavreac", "le-havre-ac");
        ALIAS.put("lehavre", "le-havre-ac");
        ALIAS.put("fcmetz", "fc-metz");
        ALIAS.put("parisfc", "paris-fc");
        ALIAS.put("parissaintgermain", "paris-saint-germain");
        ALIAS.put("ogcnice", "nice");
        ALIAS.put("losclille", "lille");
        ALIAS.put("losc", "lille");
        ALIAS.put("olympiquelyonnais", "lyon");
        ALIAS.put("olympiquedemarseille", "marseille");
        ALIAS.put("olympiquemarseille", "marseille");
        ALIAS.put("fcnantes", "nantes");
        ALIAS.put("fclorient", "lorient");
        ALIAS.put("toulousefc", "toulouse");
        ALIAS.put("ajauxerre", "auxerre");
        ALIAS.put("scoangers", "angers");
        ALIAS.put("stadernnais", "rennes");
        ALIAS.put("stadrennais", "rennes");
        ALIAS.put("athleticclub", "athletic-club");
        ALIAS.put("athleticbilbao", "athletic-club");
        ALIAS.put("atleticomadrid", "atletico-madrid");
        ALIAS.put("celtavigo", "celta");
        ALIAS.put("rccelta", "celta");
        ALIAS.put("rccdeportivo", "deportivo");
        ALIAS.put("deportivolacoruna", "deportivo");
        ALIAS.put("rcdespanyol", "espanyol");
        ALIAS.put("rcdmallorca", "mallorca");
        ALIAS.put("realbetis", "real-betis");
        ALIAS.put("realsociedad", "real-sociedad");
        ALIAS.put("rayovallecano", "rayo-vallecano");
        ALIAS.put("realoviedo", "oviedo");
        ALIAS.put("realmadrid", "real-madrid");
        ALIAS.put("elchecf", "elche");
        ALIAS.put("como1907", "como-1907");
        ALIAS.put("como", "como-1907");
        ALIAS.put("internazionale", "inter");
        ALIAS.put("intermilan", "inter");
        ALIAS.put("acmilan", "milan");
        ALIAS.put("hellasverona", "verona");
        ALIAS.put("uslecce", "lecce");
        ALIAS.put("manchestercity", "manchester-city");
        ALIAS.put("manchesterunited", "manchester-united");
        ALIAS.put("nottinghamforest", "nottingham-forest");
        ALIAS.put("brightonhovealbion", "brighton");
        ALIAS.put("tottenhamhotspur", "tottenham");
        ALIAS.put("leedsunited", "leeds-united");
        ALIAS.put("afcbournemouth", "bournemouth");
        ALIAS.put("crystalpalace", "crystal-palace");
        ALIAS.put("newcastleunited", "newcastle");
        ALIAS.put("astonvilla", "aston-villa");
        ALIAS.put("ipswichtown", "ipswich");
        ALIAS.put("hullcity", "hull-city");
        ALIAS.put("coventrycity", "coventry-city");
    }

    /**
     * @return asset-relative path (e.g. {@code icons/Prem/brentford.football-logos.cc.png})
     *         or {@code null} if no logo matches.
     */
    public static String resolveClub(Context context, String folder, String name) {
        if (folder == null || name == null) return null;
        String[] files;
        try {
            files = context.getAssets().list(folder);
        } catch (IOException e) {
            return null;
        }
        if (files == null) return null;

        Map<String, String> slugToFile = new HashMap<>();
        for (String f : files) {
            if (!f.endsWith(".png")) continue;
            String slug = f.endsWith(SUFFIX) ? f.substring(0, f.length() - SUFFIX.length())
                                             : f.substring(0, f.length() - 4);
            if (isLeagueLogo(slug)) continue;
            slugToFile.put(slug, f);
        }

        String n = norm(name);

        String alias = ALIAS.get(n);
        if (alias != null && slugToFile.containsKey(alias)) {
            return folder + "/" + slugToFile.get(alias);
        }
        for (Map.Entry<String, String> e : slugToFile.entrySet()) {
            if (norm(e.getKey()).equals(n)) return folder + "/" + e.getValue();
        }
        for (Map.Entry<String, String> e : slugToFile.entrySet()) {
            String ns = norm(e.getKey());
            if (!ns.isEmpty() && (n.contains(ns) || ns.contains(n))) {
                return folder + "/" + e.getValue();
            }
        }
        return null;
    }

    /** Builds the Glide-loadable asset URI for a resolved path. */
    public static String assetUri(String relativePath) {
        return "file:///android_asset/" + relativePath;
    }

    /** UEFA Champions League crest (asset-relative path). */
    public static final String UCL_CREST =
        "icons/tournaments_uefa-champions-league_256x256.football-logos.cc.png";

    /** Asset-relative path of a league's crest, or null. */
    public static String leagueCrest(int leagueId) {
        switch (leagueId) {
            case 177: return "icons/Prem/england_english-premier-league_256x256.football-logos.cc.png";
            case 302: return "icons/lige 1/spain_la-liga_256x256.football-logos.cc.png";
            case 78:  return "icons/bundes/germany_bundesliga_256x256.football-logos.cc.png";
            case 207: return "icons/seria a/italy_serie-a_256x256.football-logos.cc.png";
            case 168: return "icons/lige 1/france_ligue-1_256x256.football-logos.cc.png";
            default:  return null;
        }
    }

    /** Glide-loadable URI for a league crest, or null. */
    public static String leagueCrestUri(int leagueId) {
        String rel = leagueCrest(leagueId);
        return rel == null ? null : assetUri(rel);
    }

    private static boolean isLeagueLogo(String slug) {
        return slug.matches(".*_\\d+x\\d+$")
            || slug.contains("premier-league") || slug.contains("la-liga")
            || slug.contains("bundesliga") || slug.contains("serie-a")
            || slug.contains("ligue-1") || slug.contains("champions-league")
            || slug.contains("world-cup");
    }

    private static String norm(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{Mn}+", "");
        return n.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private LogoAssets() {}
}

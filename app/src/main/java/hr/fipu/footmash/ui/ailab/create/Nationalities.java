package hr.fipu.footmash.ui.ailab.create;

import java.util.Collections;
import java.util.List;

/**
 * Curated list of footballing nations, presented in the identity step's
 * dropdown. Croatia leads (this is a Croatian-language project), the rest
 * are the most common nationalities across the five seeded leagues.
 */
public final class Nationalities {

    public static final List<String> ALL = Collections.unmodifiableList(java.util.Arrays.asList(
        "Hrvatska",
        "Argentina",
        "Austrija",
        "Belgija",
        "Bosna i Hercegovina",
        "Brazil",
        "Češka",
        "Čile",
        "Danska",
        "Egipat",
        "Engleska",
        "Francuska",
        "Gana",
        "Italija",
        "Japan",
        "Južna Koreja",
        "Kolumbija",
        "Maroko",
        "Meksiko",
        "Nigerija",
        "Nizozemska",
        "Norveška",
        "Njemačka",
        "Poljska",
        "Portugal",
        "Senegal",
        "Slovenija",
        "Srbija",
        "Sjedinjene Države",
        "Škotska",
        "Španjolska",
        "Švedska",
        "Švicarska",
        "Turska",
        "Urugvaj",
        "Wales"
    ));

    private Nationalities() {}
}

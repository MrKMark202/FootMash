package hr.fipu.footmash.db;

import java.util.ArrayList;
import java.util.List;

/**
 * Clubs added on top of the bundled JSON so the seeded league set matches the
 * 2026/27 logo pack: the promoted sides for the Premier League and La Liga.
 * Rosters are representative (real core players + level-appropriate ratings) and
 * easy to tweak. Removals of the outgoing clubs are handled in {@link SeedLoader}.
 */
public final class ExtraClubs {

    public static final class XPlayer {
        public final String name, position, nationality;
        public final int age, pace, shooting, passing, dribbling, defending, physical, overall;
        XPlayer(String name, String position, String nationality, int age, int pace, int shooting,
                int passing, int dribbling, int defending, int physical, int overall) {
            this.name = name; this.position = position; this.nationality = nationality;
            this.age = age; this.pace = pace; this.shooting = shooting; this.passing = passing;
            this.dribbling = dribbling; this.defending = defending; this.physical = physical;
            this.overall = overall;
        }
    }

    public static final class XClub {
        public final String name;
        public final List<XPlayer> players;
        XClub(String name, List<XPlayer> players) { this.name = name; this.players = players; }
    }

    private static XPlayer p(String n, String pos, String nat, int age, int pa, int sh, int ps,
                            int dr, int df, int ph, int ov) {
        return new XPlayer(n, pos, nat, age, pa, sh, ps, dr, df, ph, ov);
    }

    /** Incoming clubs for the given league id, or empty if none. */
    public static List<XClub> forLeague(int leagueId) {
        List<XClub> out = new ArrayList<>();
        if (leagueId == 177) {            // Premier League
            out.add(ipswich());
            out.add(coventry());
            out.add(hull());
        } else if (leagueId == 302) {     // La Liga
            out.add(elche());
            out.add(espanyol());
            out.add(oviedo());
        }
        return out;
    }

    private static XClub ipswich() {
        List<XPlayer> p = new ArrayList<>();
        p.add(p("Alex Palmer","GK","England",28,50,22,68,40,16,74,71));
        p.add(p("Cieran Slicker","GK","Scotland",22,48,18,55,38,14,68,63));
        p.add(p("Leif Davis","LB","England",25,80,55,74,72,68,66,73));
        p.add(p("Ben Johnson","RB","England",25,78,52,70,70,70,72,70));
        p.add(p("Jacob Greaves","CB","England",24,60,40,68,58,72,74,71));
        p.add(p("Dara O'Shea","CB","Ireland",26,66,35,62,55,74,78,72));
        p.add(p("Axel Tuanzebe","CB","England",27,68,30,60,58,70,76,68));
        p.add(p("Cameron Burgess","CB","Australia",29,55,35,58,50,70,78,67));
        p.add(p("Harry Clarke","RB","England",24,74,45,64,64,66,70,66));
        p.add(p("Sam Morsy","CDM","Egypt",33,60,55,70,62,72,74,71));
        p.add(p("Jens Cajuste","CM","Sweden",25,72,58,72,70,68,80,73));
        p.add(p("Massimo Luongo","CM","Australia",32,62,58,68,64,66,70,67));
        p.add(p("Omari Hutchinson","CAM","England",21,86,68,72,84,40,64,74));
        p.add(p("Conor Chaplin","CAM","England",28,66,72,72,74,45,62,71));
        p.add(p("Jack Clarke","LW","England",24,85,66,72,82,42,62,73));
        p.add(p("Wes Burns","RM","Wales",30,84,64,66,72,55,70,70));
        p.add(p("Nathan Broadhead","ST","Wales",27,78,74,70,76,40,64,72));
        p.add(p("George Hirst","ST","England",26,70,72,58,64,38,80,69));
        p.add(p("Chiedozie Ogbene","RW","Ireland",28,90,66,64,72,50,76,71));
        p.add(p("Ali Al-Hamadi","ST","Iraq",23,80,70,56,68,35,72,66));
        return new XClub("Ipswich Town", p);
    }

    private static XClub coventry() {
        List<XPlayer> p = new ArrayList<>();
        p.add(p("Oliver Dovin","GK","Sweden",23,52,20,64,42,15,72,70));
        p.add(p("Ben Wilson","GK","England",32,46,16,52,36,13,68,63));
        p.add(p("Milan van Ewijk","RB","Netherlands",24,86,52,70,74,64,74,71));
        p.add(p("Jay Dasilva","LB","England",27,80,48,66,70,64,64,68));
        p.add(p("Bobby Thomas","CB","England",24,64,42,64,56,70,76,69));
        p.add(p("Liam Kitching","CB","England",25,62,44,64,54,70,76,69));
        p.add(p("Luis Binks","CB","England",23,66,38,64,58,68,74,67));
        p.add(p("Jake Bidwell","LB","England",32,70,40,62,58,64,68,64));
        p.add(p("Joel Latibeaudiere","RB","England",25,76,42,64,64,66,72,67));
        p.add(p("Ben Sheaf","CDM","England",27,66,55,72,64,70,74,71));
        p.add(p("Josh Eccles","CM","England",25,70,52,68,66,64,72,68));
        p.add(p("Victor Torp","CM","Norway",25,74,60,72,70,62,72,70));
        p.add(p("Jack Rudoni","CAM","England",24,78,66,72,76,52,68,72));
        p.add(p("Tatsuhiro Sakamoto","RW","Japan",28,88,62,68,82,40,62,71));
        p.add(p("Kasey Palmer","CAM","Jamaica",28,72,64,70,76,44,62,68));
        p.add(p("Jamie Allen","CM","England",30,68,55,64,64,60,70,65));
        p.add(p("Ellis Simms","ST","England",24,80,74,60,68,45,82,72));
        p.add(p("Brandon Thomas-Asante","ST","Ghana",26,82,70,60,70,42,80,70));
        p.add(p("Raphael Borges Rodrigues","LW","Brazil",21,84,62,62,78,38,64,65));
        p.add(p("Fabio Tavares","ST","Portugal",24,78,64,56,66,40,74,64));
        return new XClub("Coventry City", p);
    }

    private static XClub hull() {
        List<XPlayer> p = new ArrayList<>();
        p.add(p("Ivor Pandur","GK","Croatia",25,54,22,66,44,16,74,70));
        p.add(p("Thomas Glover","GK","Australia",27,48,18,56,38,14,70,64));
        p.add(p("Lewie Coyle","RB","England",29,76,44,64,64,66,72,67));
        p.add(p("Ryan Giles","LB","England",25,82,52,74,72,60,64,71));
        p.add(p("Charlie Hughes","CB","England",22,62,40,64,56,70,76,69));
        p.add(p("John Egan","CB","Ireland",32,58,40,62,52,72,78,70));
        p.add(p("Alfie Jones","CB","England",27,60,42,62,54,70,74,68));
        p.add(p("Cody Drameh","RB","England",23,80,44,64,68,64,70,67));
        p.add(p("Sean McLoughlin","CB","Ireland",28,56,38,58,50,68,76,66));
        p.add(p("Xavier Simons","CDM","England",22,66,50,68,62,66,72,66));
        p.add(p("Regan Slater","CM","England",25,72,52,68,68,62,70,68));
        p.add(p("Eliot Matazo","CM","Belgium",23,74,54,70,70,64,74,68));
        p.add(p("Marvin Mehlem","CAM","Germany",27,70,60,72,74,48,64,68));
        p.add(p("Liam Millar","LW","Canada",25,86,58,66,78,42,66,70));
        p.add(p("Abu Kamara","RW","England",21,88,60,64,80,40,62,69));
        p.add(p("Gustavo Puerta","CM","Colombia",21,78,55,70,72,60,70,67));
        p.add(p("Joe Gelhardt","ST","England",23,80,70,64,74,42,70,70));
        p.add(p("Mason Burstow","ST","England",22,82,66,60,68,40,74,67));
        p.add(p("Chris Bedia","ST","Ivory Coast",29,78,66,56,66,42,78,67));
        p.add(p("Mohamed Belloumi","LW","Algeria",23,84,60,62,76,38,62,66));
        return new XClub("Hull City", p);
    }

    private static XClub elche() {
        List<XPlayer> p = new ArrayList<>();
        p.add(p("Inaki Pena","GK","Spain",26,54,22,70,46,16,74,73));
        p.add(p("Matias Dituro","GK","Argentina",38,46,18,58,38,14,72,67));
        p.add(p("Pedro Bigas","CB","Spain",35,58,38,60,52,72,76,70));
        p.add(p("Victor Chust","CB","Spain",25,62,40,64,56,70,74,70));
        p.add(p("Jairo Izquierdo","LB","Spain",30,80,48,64,70,60,64,67));
        p.add(p("Alvaro Nunez","RB","Spain",25,82,46,66,70,62,70,68));
        p.add(p("Leo Petrot","LB","France",28,76,42,62,62,64,70,66));
        p.add(p("Adria Boayar","CB","Spain",21,60,36,58,52,66,72,64));
        p.add(p("Pedro Carmona","RB","Spain",23,78,44,62,66,62,70,66));
        p.add(p("Marc Aguado","CDM","Spain",25,64,52,70,64,68,74,69));
        p.add(p("Aleix Febas","CM","Spain",29,66,56,72,68,60,68,69));
        p.add(p("Rodrigo Mendoza","CM","Spain",20,76,58,72,74,58,70,71));
        p.add(p("Josan","RM","Spain",35,74,58,66,70,55,66,68));
        p.add(p("German Valera","LW","Spain",23,84,58,64,78,42,62,68));
        p.add(p("Alex Martin","CAM","Spain",24,70,60,68,72,46,64,66));
        p.add(p("Pedro Sanchez","CM","Spain",22,70,52,68,66,60,68,65));
        p.add(p("Mourad El Ghazouani","ST","Morocco",21,82,66,58,70,40,72,66));
        p.add(p("Rafa Mir","ST","Spain",28,78,70,60,66,40,78,70));
        p.add(p("Yago Santiago","LW","Spain",23,84,60,64,78,40,62,66));
        p.add(p("Diego Moreno","ST","Spain",22,80,64,56,68,38,72,64));
        return new XClub("Elche CF", p);
    }

    private static XClub espanyol() {
        List<XPlayer> p = new ArrayList<>();
        p.add(p("Marko Dmitrovic","GK","Serbia",33,54,22,68,46,16,76,74));
        p.add(p("Fernando Pacheco","GK","Spain",32,50,20,64,42,15,72,70));
        p.add(p("Leandro Cabrera","CB","Uruguay",34,58,40,64,52,74,80,73));
        p.add(p("Marash Kumbulla","CB","Albania",25,64,40,64,56,72,76,71));
        p.add(p("Omar El Hilali","RB","Morocco",21,84,48,66,72,64,70,71));
        p.add(p("Brian Olivan","LB","Spain",31,78,46,66,68,64,68,70));
        p.add(p("Fernando Calero","CB","Spain",30,60,40,64,54,72,76,71));
        p.add(p("Sergi Gomez","CB","Spain",33,56,38,62,50,70,76,69));
        p.add(p("Carlos Romero","LB","Spain",23,80,50,64,70,62,68,69));
        p.add(p("Pol Lozano","CDM","Spain",26,66,52,72,66,68,72,71));
        p.add(p("Edu Exposito","CM","Spain",29,70,62,74,72,62,70,73));
        p.add(p("Jofre Carreras","RM","Spain",24,82,56,66,74,52,64,69));
        p.add(p("Pere Milla","CAM","Spain",33,72,66,70,74,48,66,71));
        p.add(p("Nico Melamed","LW","Spain",24,84,60,66,78,44,62,69));
        p.add(p("Edu Salinas","CM","Spain",22,70,52,66,66,58,68,66));
        p.add(p("Javi Puado","CAM","Spain",27,80,74,70,78,46,66,74));
        p.add(p("Roberto Fernandez","ST","Spain",23,80,72,64,72,44,78,72));
        p.add(p("Alejo Veliz","ST","Argentina",22,82,72,60,70,45,80,71));
        p.add(p("Kike Garcia","ST","Spain",36,66,72,62,66,45,74,70));
        p.add(p("Gerard Valentin","RW","Spain",24,84,56,62,74,46,64,67));
        return new XClub("RCD Espanyol", p);
    }

    private static XClub oviedo() {
        List<XPlayer> p = new ArrayList<>();
        p.add(p("Aaron Escandell","GK","Spain",29,52,22,66,44,16,72,71));
        p.add(p("Joel Braat","GK","Netherlands",29,48,18,58,40,14,68,65));
        p.add(p("Dani Calvo","CB","Spain",31,60,40,62,54,72,76,70));
        p.add(p("David Costas","CB","Spain",28,62,40,64,54,70,74,69));
        p.add(p("Lucas Ahijado","RB","Spain",29,78,44,64,66,62,70,67));
        p.add(p("Javi Lopez","LB","Spain",30,76,42,64,64,62,68,66));
        p.add(p("Oier Luengo","CB","Spain",25,60,38,62,52,68,74,67));
        p.add(p("Nacho Vidal","RB","Spain",30,78,46,64,66,62,72,68));
        p.add(p("Rahim Alhassane","CB","Guinea",22,66,36,58,56,66,74,65));
        p.add(p("Kwasi Sibo","CDM","Equatorial Guinea",28,70,50,66,64,68,76,68));
        p.add(p("Santiago Colombatto","CM","Argentina",28,72,56,70,70,62,74,69));
        p.add(p("Santi Cazorla","CAM","Spain",40,60,66,76,76,40,58,72));
        p.add(p("Sebas Moyano","RW","Spain",28,82,58,64,74,48,64,67));
        p.add(p("Ilyas Chaira","LW","Morocco",26,84,58,64,78,42,62,68));
        p.add(p("Javi Mier","CM","Spain",25,70,52,68,66,58,68,66));
        p.add(p("Alberto Reina","CM","Spain",24,70,54,68,66,58,68,65));
        p.add(p("Salomon Rondon","ST","Venezuela",36,64,74,62,64,42,82,72));
        p.add(p("Alex Fores","ST","Spain",24,80,64,58,68,40,72,66));
        p.add(p("Borja Baston","ST","Spain",33,66,70,60,64,42,76,66));
        p.add(p("Viti Rozada","LW","Spain",22,82,56,62,74,40,60,64));
        return new XClub("Real Oviedo", p);
    }

    private ExtraClubs() {}
}

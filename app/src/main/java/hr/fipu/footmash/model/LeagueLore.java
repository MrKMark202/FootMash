package hr.fipu.footmash.model;

/**
 * Static editorial copy for the five seeded leagues: a short summary, the country
 * tagline shown under the league name, and a handful of legends who played there.
 * Keyed by the league id used throughout the app (see {@code LogoAssets}).
 */
public final class LeagueLore {

    public final String tagline;
    public final String summary;
    public final String legends;

    private LeagueLore(String tagline, String summary, String legends) {
        this.tagline = tagline;
        this.summary = summary;
        this.legends = legends;
    }

    public static LeagueLore forLeague(int leagueId) {
        switch (leagueId) {
            case 78: // Bundesliga
                return new LeagueLore(
                    "Njemačka • od 1963.",
                    "Njemački prvi razred poznat po nevjerojatnoj atmosferi, pravilu ‘50+1’ o vlasništvu navijača i najvećim prosječnim posjetama na svijetu. Bayern München dominira više od desetljeća, ali Dortmundov ‘Žuti zid’ i stalni val mladih talenata čuvaju neizvjesnost.",
                    "Gerd Müller, Franz Beckenbauer, Lothar Matthäus, Oliver Kahn, Robert Lewandowski");
            case 302: // La Liga
                return new LeagueLore(
                    "Španjolska • od 1929.",
                    "Španjolska elitna liga i povijesni dom tiki-take. El Clásico između Real Madrida i Barcelone najgledaniji je klupski susret na svijetu, dok Atlético Madrid predstavlja čvrstu treću silu.",
                    "Alfredo Di Stéfano, Lionel Messi, Cristiano Ronaldo, Raúl, Andrés Iniesta");
            case 168: // Ligue 1
                return new LeagueLore(
                    "Francuska • od 1932.",
                    "Najjača francuska liga i poznata odskočna daska za svjetske zvijezde. Paris Saint-Germain diktira tempo modernog doba, dok Marseille, Lyon i Monaco nose bogatu povijest.",
                    "Jean-Pierre Papin, Pauleta, Zlatan Ibrahimović, Edinson Cavani, Kylian Mbappé");
            case 177: // Premier League
                return new LeagueLore(
                    "Engleska • od 1992.",
                    "Najbogatija i najgledanija liga na svijetu, osnovana 1992. Brza, neumoljivo konkurentna i otvorena od prve do zadnje minute, s globalnom publikom za United, Liverpool, Arsenal, Chelsea i Manchester City.",
                    "Alan Shearer, Thierry Henry, Ryan Giggs, Wayne Rooney, Steven Gerrard");
            case 207: // Serie A
                return new LeagueLore(
                    "Italija • od 1898.",
                    "Talijanski prvi razred, dugo svjetski uzor taktičke obrane i ‘catenaccia’. Juventus, Milan i Inter oblikovali su europski nogomet uz žestoke derbije i bogatu taktičku kulturu.",
                    "Paolo Maldini, Roberto Baggio, Alessandro Del Piero, Francesco Totti, Andrea Pirlo");
            default:
                return new LeagueLore("", "", "");
        }
    }

    private LeagueLore() { this("", "", ""); }
}

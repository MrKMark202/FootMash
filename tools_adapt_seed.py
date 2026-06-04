#!/usr/bin/env python3
"""One-shot: adapt the seed JSON club set to match the provided 26/27 icon set,
and stamp every team's badge_url with its local asset path. Idempotent enough to
re-run (it rebuilds badge_url from scratch and skips clubs already present)."""
import json, os, re, unicodedata

BASE = os.path.join("app", "src", "main", "assets")
FOLDER = {
    "data/premierleague.json": "icons/Prem",
    "data/laliga.json":        "icons/Laliga",
    "data/bundesliga.json":    "icons/bundes",
    "data/seriea.json":        "icons/seria a",
    "data/ligue1.json":        "icons/lige 1",
}

def norm(s):
    s = unicodedata.normalize('NFD', s)
    s = ''.join(c for c in s if unicodedata.category(c) != 'Mn')
    return re.sub(r'[^a-z0-9]', '', s.lower())

ALIAS = {
    "bayernmunich":"bayern-munchen","1fcunionberlin":"union-berlin","unionberlin":"union-berlin",
    "1fcheidenheim":"fc-heidenheim","fcheidenheim":"fc-heidenheim","heidenheim":"fc-heidenheim",
    "1fckoln":"koln","fckoln":"koln","1fsvmainz05":"mainz-05","mainz05":"mainz-05","fsvmainz05":"mainz-05",
    "rbleipzig":"rb-leipzig","fcstpauli":"st-pauli","stpauli":"st-pauli","tsghoffenheim":"hoffenheim",
    "1899hoffenheim":"hoffenheim","scfreiburg":"freiburg","vflwolfsburg":"wolfsburg",
    "asmonaco":"as-monaco","rclens":"rc-lens","rcstrasbourg":"rc-strasbourg-alsace",
    "rcstrasbourgalsace":"rc-strasbourg-alsace","stadebrestois29":"brest","stadebrestois":"brest",
    "lehavreac":"le-havre-ac","lehavre":"le-havre-ac","fcmetz":"fc-metz","parisfc":"paris-fc",
    "parissaintgermain":"paris-saint-germain","ogcnice":"nice","losclille":"lille","losc":"lille",
    "olympiquelyonnais":"lyon","olympiquedemarseille":"marseille","olympiquemarseille":"marseille",
    "fcnantes":"nantes","fclorient":"lorient","toulousefc":"toulouse","ajauxerre":"auxerre",
    "scoangers":"angers","stadernnais":"rennes","stadrennais":"rennes","athleticclub":"athletic-club",
    "athleticbilbao":"athletic-club","atleticomadrid":"atletico-madrid","celtavigo":"celta","rccelta":"celta",
    "rccdeportivo":"deportivo","deportivolacoruna":"deportivo","rcdespanyol":"espanyol","espanyol":"espanyol",
    "rcdmallorca":"mallorca","realbetis":"real-betis","realsociedad":"real-sociedad","rayovallecano":"rayo-vallecano",
    "realoviedo":"oviedo","realmadrid":"real-madrid","elchecf":"elche","como1907":"como-1907","como":"como-1907",
    "internazionale":"inter","intermilan":"inter","acmilan":"milan","hellasverona":"verona","uslecce":"lecce",
    "manchestercity":"manchester-city","manchesterunited":"manchester-united","nottinghamforest":"nottingham-forest",
    "brightonhovealbion":"brighton","tottenhamhotspur":"tottenham","leedsunited":"leeds-united",
    "afcbournemouth":"bournemouth","crystalpalace":"crystal-palace","newcastleunited":"newcastle",
    "ipswichtown":"ipswich","hullcity":"hull-city","coventrycity":"coventry-city","astonvilla":"aston-villa",
}

def slugmap(folder):
    m = {}
    for fn in os.listdir(os.path.join(BASE, folder)):
        if not fn.endswith('.png'):
            continue
        slug = fn[:-len('.football-logos.cc.png')] if fn.endswith('.football-logos.cc.png') else fn[:-4]
        if re.search(r'_\d+x\d+$', slug) or any(k in slug for k in
                ['premier-league','la-liga','bundesliga','serie-a','ligue-1','champions-league','world-cup']):
            continue
        m[slug] = fn
    return m

def match(folder, name):
    sm = slugmap(folder)
    n = norm(name)
    if n in ALIAS and ALIAS[n] in sm:
        return sm[ALIAS[n]]
    for slug, fn in sm.items():
        if norm(slug) == n:
            return fn
    for slug, fn in sm.items():
        ns = norm(slug)
        if ns and (ns in n or n in ns):
            return fn
    return None

REMOVE = {
    "data/premierleague.json": ["West Ham United", "Wolverhampton Wanderers", "Burnley"],
    "data/laliga.json":        ["UD Las Palmas", "CD Leganes", "Real Valladolid"],
    "data/bundesliga.json":    ["Schalke 04", "Hertha BSC"],
    "data/ligue1.json":        ["Saint-Etienne", "Montpellier"],
}

# (name, pos, nat, age, pace, sho, pas, dri, def, phy, ovr)
def P(name,pos,nat,age,pa,sh,ps,dr,df,ph,ov): return (name,pos,nat,age,pa,sh,ps,dr,df,ph,ov)

SQUADS = {
 "Ipswich Town": [
  P("Alex Palmer","GK","England",28,50,22,68,40,16,74,71),
  P("Cieran Slicker","GK","Scotland",22,48,18,55,38,14,68,63),
  P("Leif Davis","LB","England",25,80,55,74,72,68,66,73),
  P("Ben Johnson","RB","England",25,78,52,70,70,70,72,70),
  P("Jacob Greaves","CB","England",24,60,40,68,58,72,74,71),
  P("Dara O'Shea","CB","Ireland",26,66,35,62,55,74,78,72),
  P("Axel Tuanzebe","CB","England",27,68,30,60,58,70,76,68),
  P("Cameron Burgess","CB","Australia",29,55,35,58,50,70,78,67),
  P("Harry Clarke","RB","England",24,74,45,64,64,66,70,66),
  P("Sam Morsy","CDM","Egypt",33,60,55,70,62,72,74,71),
  P("Jens Cajuste","CM","Sweden",25,72,58,72,70,68,80,73),
  P("Massimo Luongo","CM","Australia",32,62,58,68,64,66,70,67),
  P("Omari Hutchinson","CAM","England",21,86,68,72,84,40,64,74),
  P("Conor Chaplin","CAM","England",28,66,72,72,74,45,62,71),
  P("Jack Clarke","LW","England",24,85,66,72,82,42,62,73),
  P("Wes Burns","RM","Wales",30,84,64,66,72,55,70,70),
  P("Nathan Broadhead","ST","Wales",27,78,74,70,76,40,64,72),
  P("George Hirst","ST","England",26,70,72,58,64,38,80,69),
  P("Chiedozie Ogbene","RW","Ireland",28,90,66,64,72,50,76,71),
  P("Ali Al-Hamadi","ST","Iraq",23,80,70,56,68,35,72,66),
 ],
 "Coventry City": [
  P("Oliver Dovin","GK","Sweden",23,52,20,64,42,15,72,70),
  P("Ben Wilson","GK","England",32,46,16,52,36,13,68,63),
  P("Milan van Ewijk","RB","Netherlands",24,86,52,70,74,64,74,71),
  P("Jay Dasilva","LB","England",27,80,48,66,70,64,64,68),
  P("Bobby Thomas","CB","England",24,64,42,64,56,70,76,69),
  P("Liam Kitching","CB","England",25,62,44,64,54,70,76,69),
  P("Luis Binks","CB","England",23,66,38,64,58,68,74,67),
  P("Jake Bidwell","LB","England",32,70,40,62,58,64,68,64),
  P("Joel Latibeaudiere","RB","England",25,76,42,64,64,66,72,67),
  P("Ben Sheaf","CDM","England",27,66,55,72,64,70,74,71),
  P("Josh Eccles","CM","England",25,70,52,68,66,64,72,68),
  P("Victor Torp","CM","Norway",25,74,60,72,70,62,72,70),
  P("Jack Rudoni","CAM","England",24,78,66,72,76,52,68,72),
  P("Tatsuhiro Sakamoto","RW","Japan",28,88,62,68,82,40,62,71),
  P("Kasey Palmer","CAM","Jamaica",28,72,64,70,76,44,62,68),
  P("Jamie Allen","CM","England",30,68,55,64,64,60,70,65),
  P("Ellis Simms","ST","England",24,80,74,60,68,45,82,72),
  P("Brandon Thomas-Asante","ST","Ghana",26,82,70,60,70,42,80,70),
  P("Raphael Borges Rodrigues","LW","Brazil",21,84,62,62,78,38,64,65),
  P("Fabio Tavares","ST","Portugal",24,78,64,56,66,40,74,64),
 ],
 "Hull City": [
  P("Ivor Pandur","GK","Croatia",25,54,22,66,44,16,74,70),
  P("Thomas Glover","GK","Australia",27,48,18,56,38,14,70,64),
  P("Lewie Coyle","RB","England",29,76,44,64,64,66,72,67),
  P("Ryan Giles","LB","England",25,82,52,74,72,60,64,71),
  P("Charlie Hughes","CB","England",22,62,40,64,56,70,76,69),
  P("John Egan","CB","Ireland",32,58,40,62,52,72,78,70),
  P("Alfie Jones","CB","England",27,60,42,62,54,70,74,68),
  P("Cody Drameh","RB","England",23,80,44,64,68,64,70,67),
  P("Sean McLoughlin","CB","Ireland",28,56,38,58,50,68,76,66),
  P("Xavier Simons","CDM","England",22,66,50,68,62,66,72,66),
  P("Regan Slater","CM","England",25,72,52,68,68,62,70,68),
  P("Eliot Matazo","CM","Belgium",23,74,54,70,70,64,74,68),
  P("Marvin Mehlem","CAM","Germany",27,70,60,72,74,48,64,68),
  P("Liam Millar","LW","Canada",25,86,58,66,78,42,66,70),
  P("Abu Kamara","RW","England",21,88,60,64,80,40,62,69),
  P("Gustavo Puerta","CM","Colombia",21,78,55,70,72,60,70,67),
  P("Joe Gelhardt","ST","England",23,80,70,64,74,42,70,70),
  P("Mason Burstow","ST","England",22,82,66,60,68,40,74,67),
  P("Chris Bedia","ST","Ivory Coast",29,78,66,56,66,42,78,67),
  P("Mohamed Belloumi","LW","Algeria",23,84,60,62,76,38,62,66),
 ],
 "Elche CF": [
  P("Inaki Pena","GK","Spain",26,54,22,70,46,16,74,73),
  P("Matias Dituro","GK","Argentina",38,46,18,58,38,14,72,67),
  P("Pedro Bigas","CB","Spain",35,58,38,60,52,72,76,70),
  P("Victor Chust","CB","Spain",25,62,40,64,56,70,74,70),
  P("Jairo Izquierdo","LB","Spain",30,80,48,64,70,60,64,67),
  P("Alvaro Nunez","RB","Spain",25,82,46,66,70,62,70,68),
  P("Leo Petrot","LB","France",28,76,42,62,62,64,70,66),
  P("Adria Boayar","CB","Spain",21,60,36,58,52,66,72,64),
  P("Pedro Carmona","RB","Spain",23,78,44,62,66,62,70,66),
  P("Marc Aguado","CDM","Spain",25,64,52,70,64,68,74,69),
  P("Aleix Febas","CM","Spain",29,66,56,72,68,60,68,69),
  P("Rodrigo Mendoza","CM","Spain",20,76,58,72,74,58,70,71),
  P("Josan","RM","Spain",35,74,58,66,70,55,66,68),
  P("German Valera","LW","Spain",23,84,58,64,78,42,62,68),
  P("Alex Martin","CAM","Spain",24,70,60,68,72,46,64,66),
  P("Pedro Sanchez","CM","Spain",22,70,52,68,66,60,68,65),
  P("Mourad El Ghazouani","ST","Morocco",21,82,66,58,70,40,72,66),
  P("Rafa Mir","ST","Spain",28,78,70,60,66,40,78,70),
  P("Yago Santiago","LW","Spain",23,84,60,64,78,40,62,66),
  P("Diego Moreno","ST","Spain",22,80,64,56,68,38,72,64),
 ],
 "RCD Espanyol": [
  P("Marko Dmitrovic","GK","Serbia",33,54,22,68,46,16,76,74),
  P("Fernando Pacheco","GK","Spain",32,50,20,64,42,15,72,70),
  P("Leandro Cabrera","CB","Uruguay",34,58,40,64,52,74,80,73),
  P("Marash Kumbulla","CB","Albania",25,64,40,64,56,72,76,71),
  P("Omar El Hilali","RB","Morocco",21,84,48,66,72,64,70,71),
  P("Brian Olivan","LB","Spain",31,78,46,66,68,64,68,70),
  P("Fernando Calero","CB","Spain",30,60,40,64,54,72,76,71),
  P("Sergi Gomez","CB","Spain",33,56,38,62,50,70,76,69),
  P("Carlos Romero","LB","Spain",23,80,50,64,70,62,68,69),
  P("Pol Lozano","CDM","Spain",26,66,52,72,66,68,72,71),
  P("Edu Exposito","CM","Spain",29,70,62,74,72,62,70,73),
  P("Jofre Carreras","RM","Spain",24,82,56,66,74,52,64,69),
  P("Pere Milla","CAM","Spain",33,72,66,70,74,48,66,71),
  P("Nico Melamed","LW","Spain",24,84,60,66,78,44,62,69),
  P("Edu Salinas","CM","Spain",22,70,52,66,66,58,68,66),
  P("Javi Puado","CAM","Spain",27,80,74,70,78,46,66,74),
  P("Roberto Fernandez","ST","Spain",23,80,72,64,72,44,78,72),
  P("Alejo Veliz","ST","Argentina",22,82,72,60,70,45,80,71),
  P("Kike Garcia","ST","Spain",36,66,72,62,66,45,74,70),
  P("Gerard Valentin","RW","Spain",24,84,56,62,74,46,64,67),
 ],
 "Real Oviedo": [
  P("Aaron Escandell","GK","Spain",29,52,22,66,44,16,72,71),
  P("Joel Braat","GK","Netherlands",29,48,18,58,40,14,68,65),
  P("Dani Calvo","CB","Spain",31,60,40,62,54,72,76,70),
  P("David Costas","CB","Spain",28,62,40,64,54,70,74,69),
  P("Lucas Ahijado","RB","Spain",29,78,44,64,66,62,70,67),
  P("Javi Lopez","LB","Spain",30,76,42,64,64,62,68,66),
  P("Oier Luengo","CB","Spain",25,60,38,62,52,68,74,67),
  P("Nacho Vidal","RB","Spain",30,78,46,64,66,62,72,68),
  P("Rahim Alhassane","CB","Guinea",22,66,36,58,56,66,74,65),
  P("Kwasi Sibo","CDM","Equatorial Guinea",28,70,50,66,64,68,76,68),
  P("Santiago Colombatto","CM","Argentina",28,72,56,70,70,62,74,69),
  P("Santi Cazorla","CAM","Spain",40,60,66,76,76,40,58,72),
  P("Sebas Moyano","RW","Spain",28,82,58,64,74,48,64,67),
  P("Ilyas Chaira","LW","Morocco",26,84,58,64,78,42,62,68),
  P("Javi Mier","CM","Spain",25,70,52,68,66,58,68,66),
  P("Alberto Reina","CM","Spain",24,70,54,68,66,58,68,65),
  P("Salomon Rondon","ST","Venezuela",36,64,74,62,64,42,82,72),
  P("Alex Fores","ST","Spain",24,80,64,58,68,40,72,66),
  P("Borja Baston","ST","Spain",33,66,70,60,64,42,76,66),
  P("Viti Rozada","LW","Spain",22,82,56,62,74,40,60,64),
 ],
}

NEW_CLUBS = {
    "data/premierleague.json": ["Coventry City", "Hull City", "Ipswich Town"],
    "data/laliga.json":        ["Elche CF", "RCD Espanyol", "Real Oviedo"],
}

next_team_id = 9001
next_player_id = 90001

def build_team(name, folder):
    global next_team_id, next_player_id
    tid = next_team_id; next_team_id += 1
    players = []
    for (pn,pos,nat,age,pa,sh,ps,dr,df,ph,ov) in SQUADS[name]:
        players.append({
            "player_id": next_player_id, "name": pn, "position": pos, "nationality": nat,
            "age": age, "pace": pa, "shooting": sh, "passing": ps, "dribbling": dr,
            "defending": df, "physical": ph, "overall": ov,
        })
        next_player_id += 1
    return {"team_id": tid, "name": name, "badge_url": "", "players": players}

summary = {}
for jf, folder in FOLDER.items():
    path = os.path.join(BASE, jf)
    with open(path, encoding="utf-8") as fh:
        data = json.load(fh)
    teams = data["teams"]
    # remove
    rm = set(REMOVE.get(jf, []))
    teams = [t for t in teams if t["name"] not in rm]
    # add new clubs (skip if already present from a prior run)
    have = {t["name"] for t in teams}
    for nm in NEW_CLUBS.get(jf, []):
        if nm not in have:
            teams.append(build_team(nm, folder))
    # stamp badge_url to local asset for every team
    unmatched = []
    for t in teams:
        fn = match(folder, t["name"])
        if fn:
            t["badge_url"] = "file:///android_asset/" + folder + "/" + fn
        else:
            unmatched.append(t["name"])
    data["teams"] = teams
    text = json.dumps(data, ensure_ascii=False, indent=2)
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(text)
    summary[jf] = (len(teams), unmatched)

print("RESULT:")
for jf,(n,um) in summary.items():
    print(f"  {jf}: {n} teams | unmatched badges: {um}")
print("next ids used -> team:", next_team_id-1, "player:", next_player_id-1)

# FootMash — Master Plan

**Date:** 2026-05-12  
**Author:** Claude Sonnet 4.6 (Anthropic)  
**Scope:** Full codebase review + seed data plan + AI season simulation game design

---

---

# Part 1 — Codebase Review

## 1. Project Purpose

FootMash is an Android football (soccer) information and simulation app. Users can browse real-world leagues, teams, and players from global competitions. The standout feature is the **AI Lab**, powered by Google Gemini 1.5 Flash, which simulates how user-created custom players and teams would perform in real leagues.

**Target audience:** Football fans, fantasy football players, AI simulation enthusiasts  
**Language:** Croatian (UI text)

---

## 2. Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 11 |
| Platform | Android | Min SDK 28 / Target 29 / Compile 34 |
| Build | Gradle + AGP | 8.3.2 |
| Networking | Retrofit + OkHttp3 + Gson | 2.9.0 / 4.12.0 / 2.10.1 |
| Image Loading | Glide | 4.16.0 |
| Local DB | Room (SQLite) | 2.6.1 |
| UI | AndroidX Material + ConstraintLayout + RecyclerView | 1.11.0 / 2.1.4 / 1.3.2 |
| Navigation | AndroidX Navigation Component | 2.7.7 |
| Reactive | ViewModel + LiveData | 2.7.0 |
| AI | Google Gemini 1.5 Flash | via REST API |
| Testing | JUnit + Espresso | 4.13.2 / 3.7.0 |

---

## 3. Architecture

The app follows a layered **MVVM** architecture:

```
Fragments / Activities  (UI Layer)
        ↓
    ViewModels + LiveData  (Presentation Layer)
        ↓
    FootballRepository  (Domain / Abstraction Layer)
        ↓              ↓
  AllSportsAPI       Room DB
  GeminiAPI        (CustomPlayer, CustomTeam)
```

**Single-Activity pattern:** `MainActivity` hosts all fragments via AndroidX Navigation Component and a `BottomNavigationView`.

---

## 4. Module Breakdown

| Module | Path | Description |
|---|---|---|
| Home | `ui/home/` | Featured leagues screen (hardcoded Premier League + Cup IDs) |
| Leagues | `ui/leagues/` | Browse leagues by country |
| Teams | `ui/teams/` | Browse teams within a selected league |
| Players | `ui/players/` | Player details within a team |
| AI Lab | `ui/ailab/` | Create custom players/teams and simulate against real leagues |
| API Client | `api/` | Retrofit singleton for AllSportsAPI |
| AI Client | `ai/` | Retrofit singleton + repository for Google Gemini API |
| Database | `db/` | Room database with DAOs for CustomPlayer and CustomTeam |
| Repository | `repository/` | Single data access abstraction layer |

---

## 5. Data Model

### API Entities (AllSportsAPI — read-only, not cached)
- `CountryResponse` — country name and code
- `LeagueResponse` — league name, logo, country reference
- `TeamResponse` — team details + player list
- `PlayerResponse` — player stats (position, age, goals, cards, etc.)
- `StandingResponse` — league table (position, points, W/D/L, GD)
- `TopScorerResponse` — top scorers *(declared, currently unused)*
- `TeamStatisticsResponse` — team-level stats *(declared, currently unused)*

### Local Entities (Room — user-created, persisted)
- `CustomPlayer` — firstName, lastName, age, position, 6 attributes (pace, shooting, passing, dribbling, defending, physical), target team/league reference
- `CustomTeam` — name, formation, average stats derived from players, player ID list, target league reference

---

## 6. Navigation Graph

| Destination | Fragment |
|---|---|
| Start | HomeFragment |
| Leagues/Countries | LeaguesFragment / CountriesFragment |
| Standings | StandingsFragment |
| Teams | TeamsFragment |
| Team Detail | TeamDetailFragment |
| Players | PlayersFragment |
| Player Detail | PlayerDetailFragment |
| AI Lab | AILabFragment |
| Player Simulation | PlayerSimulationFragment |
| Team Simulation | TeamSimulationFragment |

---

## 7. Configuration

API keys are read from `local.properties` (git-ignored) and injected into `BuildConfig` at build time:

```properties
GEMINI_API_KEY=...
FOOTBALL_API_KEY=...
```

API base URLs:
- Football: `https://apiv2.allsportsapi.com/football/`
- Gemini: `https://generativelanguage.googleapis.com/v1beta/`

Network timeouts: 30s (connect + read). HTTP logging interceptor active at BODY level.

---

## 8. Design Patterns

| Pattern | Where Used |
|---|---|
| Singleton | ApiClient, GeminiClient, AppDatabase |
| Repository | FootballRepository, GeminiRepository |
| MVVM | All ViewModels + LiveData observers |
| Adapter | RecyclerView adapters (Leagues, Teams, Players, etc.) |
| Fragment Navigation | NavController + NavHostFragment |
| View Binding | All layouts (no `findViewById`) |

---

## 9. Testing

**Current state: Minimal**

Only placeholder tests exist:
- `ExampleUnitTest.java` — JUnit 4 placeholder
- `ExampleInstrumentedTest.java` — AndroidJUnit4 placeholder

No actual test coverage for repositories, ViewModels, API logic, or UI flows.

---

## 10. Issues & Concerns

### High Priority

| # | Issue | Details |
|---|---|---|
| H1 | **API keys in BuildConfig** | Keys are embedded in the APK binary. Anyone can decompile and extract them. Should be proxied through a backend or secured via a secrets manager. |
| H2 | **No error handling for API failures** | Failed Retrofit calls return empty lists silently. Users see blank screens with no feedback. |
| H3 | **No offline support** | Room is set up but API data is never cached. App is non-functional without internet. |

### Medium Priority

| # | Issue | Details |
|---|---|---|
| M1 | **Hardcoded values** | Premier League ID (`177`), Cup ID (`423`), and season (`2024`) are hardcoded in HomeFragment. |
| M2 | **No pagination** | All leagues, teams, and players are fetched in one request. Performance will degrade with large datasets. |
| M3 | **No Gemini rate limiting or caching** | Every simulation triggers a fresh API call. No retry logic, no request deduplication, no response caching. |
| M4 | **Unused models** | `TopScorerResponse`, `TeamStatisticsResponse`, `ApiResponse` are declared but never used. |
| M5 | **Hardcoded Gemini prompts** | Simulation prompts are not configurable. Any change requires a code rebuild. |

### Low Priority

| # | Issue | Details |
|---|---|---|
| L1 | **No Room migrations** | Database schema is version 1 with no migration strategies. Any schema change will require destructive recreation. |
| L2 | **No CI/CD** | No automated build, test, or lint pipelines configured. |
| L3 | **Minimal code comments** | Sparse comments, mostly in Croatian. No architecture documentation. |
| L4 | **Build error artifacts** | `build_errors.txt`, `cmd_output*.txt` files committed to repo root — these should be git-ignored. |
| L5 | **Memory leak risk** | Some fragments hold adapter references without explicit cleanup. LiveData observer lifecycle management should be audited. |

---

## 11. Strengths

- Clean MVVM separation with a proper Repository abstraction layer
- Single-Activity navigation with AndroidX Navigation Component
- View Binding used consistently — no `findViewById` boilerplate
- Modern AndroidX library stack throughout
- Interesting and differentiating AI Lab concept with Gemini integration
- Modular, readable fragment structure

---

## 12. Recommended Next Steps

1. **Proxy API keys** through a lightweight backend or use Android Keystore / secrets Gradle plugin to avoid binary exposure
2. **Add error states** to all ViewModels — expose loading/error/success states via sealed classes or StateFlow
3. **Implement offline caching** — cache API responses in Room for core screens (leagues, standings)
4. **Add unit tests** for Repository and ViewModel layers using JUnit + MockK/Mockito
5. **Remove pagination gap** — implement paging for Teams and Players lists (AndroidX Paging 3)
6. **Clean up repo root** — add `*.txt` output files to `.gitignore`
7. **Add Room migrations** — define migration strategies before first public release
8. **Externalize Gemini prompts** — move simulation prompts to a config file or remote config

---

---

# Part 2 — Real-World League Data Plan

**Scope:** Premier League, La Liga — teams + players with full attributes  
**Status:** Planning (no code written)

---

## The Core Problem

The app already has AllSportsAPI integrated and calls it live for teams and players.  
However, AllSportsAPI returns **statistical data** (goals, cards, matches played) — it does **not** return the FIFA-style attribute scores that the AI Lab simulation is built around:

```
pace | shooting | passing | dribbling | defending | physical
```

These 6 attributes are the entire foundation of `CustomPlayer` and `CustomTeam`.  
Without them for real players, the AI Lab has no baseline to compare your custom player against.

**This is the actual gap** — not that teams/players are missing, but that real players lack simulation attributes.

---

## Why Not Just Static JSON Files?

Static JSON files per league (`premierleague.json`, `laliga.json`) are the simplest approach and will work, but come with trade-offs:

| | Static JSON in assets/ | Live AllSportsAPI | Hybrid (Recommended) |
|---|---|---|---|
| Offline access | Yes | No | Yes (seed data) |
| Always up-to-date | No (manual updates) | Yes | Partial |
| FIFA-style attributes | Yes (you define them) | No (not available) | Yes (from seed) |
| Works with AI Lab | Yes | No (wrong data shape) | Yes |
| Build size impact | +~500KB per league | None | +~500KB per league |
| Maintenance burden | High (manual edits) | None | Low |
| API key required | No | Yes | No (for seed data) |

---

## Recommended Approach: Static JSON Seed Data + Room DB

### How it works

1. JSON files live in `app/src/main/assets/data/` — one per league
2. On first app launch, a `SeedLoader` reads the JSON files and inserts the data into **two new Room tables**: `real_teams` and `real_players`
3. The AI Lab simulation reads from `real_players` to get attribute baselines for comparison
4. Live API calls remain unchanged — they still power Leagues, Standings, and Teams browsing screens
5. No internet required for AI Lab to function

### What the JSON files provide vs what the API provides

```
API (AllSportsAPI) — unchanged, stays live
  └── Countries, Leagues, Standings, Top Scorers, Match data

JSON Seed Files — new, loaded once into Room
  └── real_teams:  team_id, name, badge_url, league, country
  └── real_players: player_id, name, position, nationality, age,
                    pace, shooting, passing, dribbling, defending, physical,
                    overall, team_id, league_id
```

---

## JSON File Structure

### File locations
```
app/src/main/assets/
  data/
    premierleague.json
    laliga.json
    (future: bundesliga.json, seriea.json, ligue1.json, ...)
```

### Schema: `premierleague.json`
```json
{
  "league": {
    "id": 177,
    "name": "Premier League",
    "country": "England",
    "season": 2024
  },
  "teams": [
    {
      "team_id": 85,
      "name": "Manchester City",
      "badge_url": "https://apiv2.allsportsapi.com/logo/85_manchester-city.jpg",
      "players": [
        {
          "player_id": 1001,
          "name": "Erling Haaland",
          "position": "FW",
          "nationality": "Norway",
          "age": 24,
          "pace": 89,
          "shooting": 94,
          "passing": 65,
          "dribbling": 80,
          "defending": 45,
          "physical": 88,
          "overall": 91
        }
      ]
    }
  ]
}
```

---

## Leagues to Seed (Phase 1)

| File | League | AllSportsAPI ID | Teams | Players (est.) |
|---|---|---|---|---|
| `premierleague.json` | Premier League | 177 | 20 | ~500 |
| `laliga.json` | La Liga | 302 | 20 | ~500 |

**Phase 2 (future):** `bundesliga.json` (78), `seriea.json` (207), `ligue1.json` (168)

---

## Room DB Changes Required (Seed Data)

### `RealPlayer` entity
```
id (PK), name, position, nationality, age,
pace, shooting, passing, dribbling, defending, physical, overall,
teamId, teamName, leagueId, leagueName, marketValue
```

### `RealTeam` entity
```
id (PK), name, badgeUrl, leagueId, leagueName, country
```

---

## SeedLoader Logic (first-launch only)

```
App launches
  → Check SharedPreferences: "seed_loaded" flag
  → If false:
      → Read premierleague.json from assets → bulk insert into Room
      → Read laliga.json from assets → bulk insert into Room
      → Set "seed_loaded" = true
  → If true: skip
```

---

---

# Part 3 — AI Season Simulation Game

**This is the core game mechanic of FootMash.**

---

## Concept Overview

The user is a football manager. They pick a league, are given a transfer budget, and must **draft a squad of real players** within that budget. Once they confirm their lineup and formation, the AI simulates every single match of the season — producing scorelines, goal scorers, and a final standings table with a league winner and golden boot winner.

---

## Full User Flow

```
1. START NEW SEASON
   └── Pick a league (Premier League / La Liga / ...)
   └── Name your club
   └── Receive transfer budget (e.g. €100M)

2. TRANSFER MARKET (Draft)
   └── Browse real players from that league's seed data
   └── Filter by position (GK / DF / MF / FW)
   └── See each player's: name, overall, attributes, price
   └── Add players to squad (budget decreases per signing)
   └── Minimum squad: 11 players (GK + outfield)
   └── Recommended: 18–23 players (subs + rotation)

3. SQUAD & FORMATION SETUP
   └── Pick formation: 4-3-3 / 4-4-2 / 3-5-2 / 4-2-3-1 / etc.
   └── Drag players into positions on a pitch view
   └── Confirm starting XI + substitutes

4. START THE SEASON
   └── App generates full fixture list (38 matchdays for 20 teams)
   └── User's team enters the league alongside the 19 real teams

5. SIMULATE MATCHDAYS
   └── User taps "Simulate Matchday X"
   └── App sends all matches of that matchday to Gemini
   └── Gemini returns structured JSON: scoreline + goal scorers for each match
   └── Results are saved to Room DB
   └── Standings + top scorers table update in real time

6. SEASON PROGRESS
   └── User can view: current standings, top scorers, upcoming fixtures, past results
   └── Repeat Step 5 for all 38 matchdays

7. SEASON END
   └── League winner announced (most points)
   └── Golden Boot winner announced (most goals)
   └── Season summary screen with user team's final position
```

---

## Player Pricing System

Player prices are based on their overall rating from the seed data. This keeps the draft balanced — buying the best players costs most of the budget.

| Overall Rating | Market Value |
|---|---|
| 90+ | €55M – €80M | |
| 85 – 89 €30M – €55M |
| 80 – 84 | €15M – €30M |
| 75 – 79 | €6M – €15M |
| 70 – 74 | €2M – €6M |
| Below 70 | €500K – €2M |

Starting budget: **€100M** (adjustable per difficulty setting in the future).

A squad of 18 players at average €5M each costs €90M — the budget forces real trade-offs between depth and quality, exactly like a real draft.

---

## Fixture Generation (No AI Needed)

A full double round-robin schedule (each team plays every other team home and away) is generated algorithmically — no Gemini call required for this step.

```
20 teams → 38 matchdays → 10 matches per matchday → 380 total matches

Algorithm: standard "circle method" round-robin scheduling
- Fix team[0] (user's team), rotate the remaining 19 each round
- First half of season = first leg (home/away)
- Second half = reversed fixtures
```

All 380 fixtures are generated at season start and saved to the `fixtures` Room table immediately.

---

## How AI Simulates Each Match

This is the core intelligence. For each match, Gemini receives a structured prompt with both teams' data and must return a structured JSON response.

### What is sent to Gemini (per match)

```
Home team:
  Name: FC Nova | Formation: 4-3-3
  Overall avg: 79
  Starting XI:
    GK: Alisson (overall 89)
    DF: Alexander-Arnold (overall 87), Van Dijk (overall 90), ...
    MF: Rodri (overall 91), ...
    FW: Salah (overall 90), ...

Away team:
  Name: Arsenal | Formation: 4-2-3-1
  Overall avg: 84
  Starting XI:
    GK: Raya (overall 83)
    ...

Instructions: Simulate this football match. Return ONLY valid JSON in this exact format:
{
  "home_goals": <number>,
  "away_goals": <number>,
  "scorers": [
    { "name": "<player name>", "team": "home" or "away", "minute": <1-90> }
  ]
}
No text outside the JSON. Scorers list must match the goal counts exactly.
```

### What Gemini returns

```json
{
  "home_goals": 2,
  "away_goals": 1,
  "scorers": [
    { "name": "Salah",     "team": "home", "minute": 23 },
    { "name": "Salah",     "team": "home", "minute": 67 },
    { "name": "Martinelli","team": "away", "minute": 89 }
  ]
}
```

The app parses this JSON, saves the result to `match_results`, updates the standing for both teams, and adds each goal to the `goal_scorers` tally.

### Batching to reduce API calls

Instead of one Gemini call per match (380 calls per season), send an entire matchday at once — 10 matches in a single prompt, expecting 10 JSON objects back.

```
380 matches / 10 per matchday = 38 Gemini calls for a full season
```

This is fast, cheap, and keeps the season simulation practical.

---

## Room DB — New Tables for Game Mode

The following tables are added on top of the existing `custom_players`, `custom_teams`, `real_players`, `real_teams`:

### `user_club`
```
id (PK, autoGenerate)
clubName         String
leagueId         int
leagueName       String
formation        String       -- e.g. "4-3-3"
budget           long         -- remaining budget in euros
seasonYear       int          -- e.g. 2025
isActive         boolean      -- only one active season at a time
```

### `user_squad`
```
id (PK, autoGenerate)
clubId           int          -- FK → user_club
playerId         int          -- FK → real_players
isStartingXI     boolean
pitchPosition    String       -- e.g. "LW", "CDM", "CB"
```

### `fixture`
```
id (PK, autoGenerate)
seasonId         int          -- FK → user_club
matchday         int          -- 1–38
homeTeamId       int
homeTeamName     String
awayTeamId       int
awayTeamName     String
isUserTeam       boolean      -- true if user's club is in this match
isSimulated      boolean      -- false until Gemini returns result
```

### `match_result`
```
id (PK, autoGenerate)
fixtureId        int          -- FK → fixture
homeGoals        int
awayGoals        int
matchSummary     String       -- raw Gemini JSON stored for debug
```

### `goal_scorer`
```
id (PK, autoGenerate)
seasonId         int
fixtureId        int
playerName       String
teamName         String
minute           int
isUserTeamPlayer boolean
```

### `season_standing`
```
id (PK, autoGenerate)
seasonId         int
teamId           int
teamName         String
played           int
won              int
drawn            int
lost             int
goalsFor         int
goalsAgainst     int
points           int
isUserTeam       boolean
```

---

## New Screens Required

| Screen | Description |
|---|---|
| `LeaguePickerFragment` | User picks which league to play in |
| `ClubSetupFragment` | User names their club |
| `TransferMarketFragment` | Browse + buy real players within budget |
| `SquadBuilderFragment` | Set formation, drag players into positions |
| `SeasonHubFragment` | Central dashboard: standings, next matchday, recent results |
| `MatchdayFragment` | Shows all 10 matches for a matchday, simulate button |
| `MatchResultFragment` | Shows scoreline + goal events after simulation |
| `StandingsFragment` (updated) | Live season table with user team highlighted |
| `TopScorersFragment` (updated) | Season golden boot race |
| `SeasonSummaryFragment` | End-of-season screen — champion + golden boot |

---

## Updated Navigation Graph

```
HomeFragment
  └── New Season → LeaguePickerFragment
        └── ClubSetupFragment
              └── TransferMarketFragment
                    └── SquadBuilderFragment
                          └── SeasonHubFragment
                                ├── MatchdayFragment → MatchResultFragment
                                ├── StandingsFragment
                                └── TopScorersFragment
                          └── (season end) SeasonSummaryFragment
```

---

## Standings Calculation Logic

After every simulated match, the standing for both teams is recalculated from scratch from all `match_results` for that season. This avoids incremental update bugs.

```
For each team in the league:
  played  = count of simulated fixtures involving this team
  won     = count where this team scored more
  drawn   = count where scores were equal
  lost    = count where this team scored fewer
  gf      = sum of goals scored by this team
  ga      = sum of goals conceded
  points  = (won × 3) + drawn
  gd      = gf - ga

Sort by: points DESC → gd DESC → gf DESC
```

---

## Top Scorers Calculation

After every matchday, the top scorers table is rebuilt from the `goal_scorer` table:

```
SELECT playerName, teamName, COUNT(*) as goals
FROM goal_scorer
WHERE seasonId = :currentSeason
GROUP BY playerName, teamName
ORDER BY goals DESC
LIMIT 20
```

---

## Simulation Prompt Strategy — Key Rules for Gemini

To get reliable structured output from Gemini, the prompt must follow these rules:

1. **Demand JSON only** — "Return ONLY valid JSON. No text before or after."
2. **Provide the exact schema** — show the expected JSON structure in the prompt
3. **Scorers must match goal count** — "The scorers array must contain exactly home_goals + away_goals entries"
4. **Use overall ratings to guide realism** — higher overall team should win more often but not always
5. **Scorers must be from the provided starting XI** — "Goal scorers must be players listed in the starting XI above"
6. **Cap goals per match** — "Total goals per match should be between 0 and 6" (prevents 9-0 absurdities)

---

## Error Handling for Simulation

Gemini occasionally returns malformed JSON. The app must handle this gracefully:

```
Parse Gemini response
  → If valid JSON: save result, update standings
  → If JSON parse fails:
      → Retry once with simplified prompt
      → If retry fails: generate result locally using a simple
        probability model based on team overall averages
        (home team wins 45% / draw 25% / away wins 30%, weighted by rating gap)
      → Mark result as "auto-generated" in match_result table
```

This ensures the season always progresses even if Gemini has an off response.

---

## Implementation Phases

### Phase 0 — Content (no code)
- Generate `premierleague.json` and `laliga.json` with player attributes
- 20 teams × 25 players each, EAFC-based ratings

### Phase 1 — Data Foundation
- Add `RealPlayer`, `RealTeam` Room entities + DAOs
- Bump `AppDatabase` to version 2 with migration
- Implement `SeedLoader` (reads JSON assets → inserts into Room on first launch)

### Phase 2 — Club & Draft
- `LeaguePickerFragment` + `ClubSetupFragment`
- `TransferMarketFragment` — browse players, filter by position, buy within budget
- Add `user_club` + `user_squad` Room tables + DAOs

### Phase 3 — Squad Builder
- `SquadBuilderFragment` — formation picker + player placement
- Pitch view showing 11 positions, drag/drop or tap-to-assign
- Validation: must have GK + 10 outfield before confirming

### Phase 4 — Season Engine
- Fixture generation algorithm (round-robin, 38 matchdays)
- Add `fixture`, `match_result`, `goal_scorer`, `season_standing` Room tables
- `SeasonHubFragment` — dashboard showing current state

### Phase 5 — AI Match Simulation
- `MatchdayFragment` — list of 10 matches, "Simulate Matchday" button
- Gemini prompt builder: formats both teams' starting XI + attributes into structured prompt
- Gemini response parser: extracts scoreline + goal scorers from JSON
- Fallback local simulator for when Gemini fails
- Standings + top scorer recalculation after each matchday

### Phase 6 — Season End
- Detect when all 38 matchdays are simulated
- `SeasonSummaryFragment` — champion, relegated teams, golden boot, user team final position
- Option to start a new season

---

## Files That Will Change or Be Created

### New Java files
| File | Purpose |
|---|---|
| `model/RealPlayer.java` | Room entity — seeded real player with attributes |
| `model/RealTeam.java` | Room entity — seeded real team |
| `model/UserClub.java` | Room entity — user's club for current season |
| `model/UserSquad.java` | Room entity — user's squad entry (player + position) |
| `model/Fixture.java` | Room entity — scheduled match |
| `model/MatchResult.java` | Room entity — simulated match result |
| `model/GoalScorer.java` | Room entity — goal event in a match |
| `model/SeasonStanding.java` | Room entity — live league table row |
| `db/RealPlayerDao.java` | DAO for RealPlayer |
| `db/RealTeamDao.java` | DAO for RealTeam |
| `db/UserClubDao.java` | DAO for UserClub + UserSquad |
| `db/FixtureDao.java` | DAO for Fixture + MatchResult + GoalScorer |
| `db/StandingDao.java` | DAO for SeasonStanding |
| `repository/SeasonRepository.java` | Orchestrates game logic: fixture gen, simulation, standings |
| `ai/MatchSimulator.java` | Builds Gemini prompt + parses match result JSON |
| `ai/LocalSimulator.java` | Fallback probability-based simulator |
| `ui/season/LeaguePickerFragment.java` | League selection screen |
| `ui/season/ClubSetupFragment.java` | Club name entry |
| `ui/season/TransferMarketFragment.java` | Draft/buy players screen |
| `ui/season/SquadBuilderFragment.java` | Formation + lineup screen |
| `ui/season/SeasonHubFragment.java` | Season dashboard |
| `ui/season/MatchdayFragment.java` | Matchday simulate screen |
| `ui/season/MatchResultFragment.java` | Post-match result screen |
| `ui/season/SeasonSummaryFragment.java` | End-of-season screen |

### Modified files
| File | Change |
|---|---|
| `db/AppDatabase.java` | Add all new entities, bump to version 3, add migration |
| `FootMashApp.java` | Trigger SeedLoader on first launch |
| `ai/GeminiRepository.java` | Add `simulateMatch()` method |
| `ui/MainActivity.java` | Add new nav destinations to bottom nav |
| `res/navigation/nav_graph.xml` | Add all new fragment destinations |
| `app/src/main/assets/data/premierleague.json` | New seed data file |
| `app/src/main/assets/data/laliga.json` | New seed data file |

---

*This plan covers all three layers: codebase review, seed data, and the full game simulation engine. Implementation starts with Phase 0 (content) before any code is written.*

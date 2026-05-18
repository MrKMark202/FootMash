# FootMash — Complete Project Explanation

**Platform:** Android (Java)  
**Min SDK:** 28 | **Target SDK:** 29 | **Compile SDK:** 34  
**Architecture:** Single-Activity MVVM  
**UI Language:** Croatian

---

## Table of Contents

1. [Project Purpose](#1-project-purpose)
2. [Tech Stack](#2-tech-stack)
3. [Architecture Overview](#3-architecture-overview)
4. [Project Structure](#4-project-structure)
5. [Database Schema](#5-database-schema)
6. [API Integration](#6-api-integration)
7. [AI Integration (Gemini)](#7-ai-integration-gemini)
8. [Seed Data System](#8-seed-data-system)
9. [Season Mode — Full Game Flow](#9-season-mode--full-game-flow)
10. [Navigation Graph](#10-navigation-graph)
11. [Key Patterns & Conventions](#11-key-patterns--conventions)
12. [File Reference](#12-file-reference)

---

## 1. Project Purpose

FootMash is an Android football (soccer) manager simulation game. It combines two distinct experiences:

**Browse Mode** — live data from the AllSports API lets users browse real leagues, teams, players, and standings from competitions like the Premier League and La Liga.

**Season Mode** — the core game. The user creates a club, signs real players from seed data within a transfer budget, arranges a starting XI on a formation pitch view, then simulates an entire 38-matchday season using Google Gemini AI to generate realistic scorelines and goal scorers. A full league table, top scorers chart, and end-of-season summary are produced.

---

## 2. Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 11 |
| Build | Gradle + AGP | 8.3.2 |
| Networking | Retrofit + OkHttp3 + Gson | 2.9.0 / 4.12.0 / 2.10.1 |
| Image Loading | Glide | 4.16.0 |
| Local DB | Room (SQLite) | 2.6.1 |
| UI Components | AndroidX Material + ConstraintLayout + RecyclerView | 1.11.0 / 2.1.4 / 1.3.2 |
| Navigation | AndroidX Navigation Component | 2.7.7 |
| Reactive | ViewModel + LiveData | 2.7.0 |
| AI | Google Gemini 1.5 Flash | via REST API |

**API keys** are defined in `local.properties` (git-ignored) and injected into `BuildConfig` at build time:
```
GEMINI_API_KEY=...
FOOTBALL_API_KEY=...
```

---

## 3. Architecture Overview

```
Fragment / Activity  (UI Layer — observes LiveData, sends user events)
        ↓
    ViewModel        (Presentation Layer — holds state, calls repository)
        ↓
    Repository       (Domain Layer — orchestrates data sources)
        ↓         ↓
  Room DB       Retrofit APIs  (AllSports + Gemini)
```

**Single-Activity pattern:** `MainActivity` hosts a `NavHostFragment` and a `BottomNavigationView`. All screens are `Fragment` subclasses connected by `NavController`. The bottom nav is hidden automatically on detail/sub-screens via a destination-change listener.

**ViewBinding** is used throughout — no `findViewById` anywhere in the codebase.

**LiveData** bridges the ViewModel and Fragment. Room DAOs return `LiveData<T>` for queries that need to react to database writes. Blocking (sync) query variants are used on background threads where needed.

---

## 4. Project Structure

```
app/src/main/java/hr/fipu/footmash/
│
├── FootMashApp.java              — Application subclass, triggers SeedLoader
│
├── api/
│   ├── ApiClient.java            — Retrofit singleton for AllSports API
│   └── FootballApi.java          — Retrofit interface (leagues, teams, players, standings)
│
├── ai/
│   ├── GeminiClient.java         — Retrofit singleton for Gemini REST API
│   ├── GeminiApi.java            — Retrofit interface (generateContent endpoint)
│   ├── GeminiRequest.java        — Request body POJO (contents → parts → text)
│   ├── GeminiResponse.java       — Response body POJO (candidates → content → parts)
│   ├── GeminiRepository.java     — Async (LiveData) + sync (blocking) Gemini callers
│   ├── MatchSimulator.java       — Prompt builder + JSON response parser for match sim
│   └── LocalSimulator.java       — Probability-based fallback simulator (no API needed)
│
├── model/
│   │   — API response models (read-only, never stored in Room)
│   ├── AllSportsResponse.java
│   ├── AllSportsStandingsResponse.java
│   ├── ApiResponse.java
│   ├── CountryResponse.java
│   ├── LeagueResponse.java
│   ├── TeamResponse.java
│   ├── PlayerResponse.java
│   ├── StandingResponse.java
│   ├── TopScorerResponse.java
│   ├── TeamStatisticsResponse.java
│   │
│   │   — AI Lab models (Room entities, user-created)
│   ├── CustomPlayer.java
│   ├── CustomTeam.java
│   │
│   │   — Seed data models (Room entities, loaded from JSON assets)
│   ├── RealPlayer.java
│   ├── RealTeam.java
│   │
│   │   — Season Mode models (Room entities, created during gameplay)
│   ├── UserClub.java
│   ├── UserSquad.java
│   ├── FormationSlot.java        — pitch position descriptor (not a Room entity)
│   ├── Fixture.java
│   ├── MatchResult.java
│   ├── GoalScorer.java
│   └── SeasonStanding.java
│
├── db/
│   ├── AppDatabase.java          — Room database singleton, version 4, all migrations
│   ├── Converters.java           — Room TypeConverters (List<Integer> ↔ String)
│   ├── SeedLoader.java           — Reads JSON assets, bulk-inserts into Room (once)
│   ├── CustomPlayerDao.java
│   ├── CustomTeamDao.java
│   ├── RealPlayerDao.java
│   ├── RealTeamDao.java
│   ├── UserClubDao.java          — UserClub + UserSquad queries
│   ├── FixtureDao.java           — Fixture + MatchResult + GoalScorer queries
│   ├── StandingDao.java          — SeasonStanding + top scorer aggregate query
│   └── TopScorerRow.java         — POJO for the top scorers GROUP BY query result
│
├── repository/
│   ├── FootballRepository.java   — AllSports API calls (leagues, teams, players, etc.)
│   └── SeasonRepository.java     — Season game logic (fixture gen, simulation, standings)
│
└── ui/
    ├── MainActivity.java
    │
    ├── home/
    │   ├── HomeFragment.java
    │   └── HomeViewModel.java
    │
    ├── leagues/
    │   ├── CountriesFragment.java + CountriesAdapter.java
    │   ├── LeaguesFragment.java + LeaguesAdapter.java + LeaguesViewModel.java
    │   ├── StandingsFragment.java + StandingsAdapter.java + StandingsViewModel.java
    │
    ├── teams/
    │   ├── TeamsFragment.java + TeamsAdapter.java + TeamsViewModel.java
    │   └── TeamDetailFragment.java
    │
    ├── players/
    │   ├── PlayersFragment.java + PlayersAdapter.java + PlayersViewModel.java
    │   └── PlayerDetailFragment.java
    │
    ├── ailab/
    │   ├── AILabFragment.java
    │   ├── PlayerSimulationFragment.java + PlayerSimulationViewModel.java
    │   ├── TeamSimulationFragment.java + TeamSimulationViewModel.java
    │   └── SearchAdapter.java
    │
    └── season/
        ├── LeaguePickerFragment.java     — Phase 2: pick Premier League or La Liga
        ├── ClubSetupFragment.java        — Phase 2: name the club, allocate budget
        ├── TransferMarketFragment.java   — Phase 2: sign players within budget
        ├── TransferMarketAdapter.java    — Phase 2: RecyclerView for player market
        ├── TransferMarketViewModel.java  — Phase 2: signed players + budget state
        ├── SquadBuilderFragment.java     — Phase 3: formation picker + pitch view
        ├── SquadBuilderViewModel.java    — Phase 3: slot assignments, formation map
        ├── SeasonHubFragment.java        — Phase 4: season dashboard
        ├── SeasonHubViewModel.java       — Phase 4: standings, next fixture, matchday
        ├── MatchdayFragment.java         — Phase 5: simulate button + results list
        ├── MatchdayAdapter.java          — Phase 5: fixture cards pre/post simulation
        ├── MatchdayViewModel.java        — Phase 5: simulation state + fixture display
        ├── SeasonSummaryFragment.java    — Phase 6: end-of-season screen
        └── SeasonSummaryViewModel.java   — Phase 6: champion, golden boot, user result
```

---

## 5. Database Schema

The Room database (`footmash_database`) is at **version 4** with three migrations. All tables are listed below.

### Pre-existing tables (versions 1–2)

**`custom_players`** — user-created players for the AI Lab  
Fields: `id`, `firstName`, `lastName`, `age`, `position`, `pace`, `shooting`, `passing`, `dribbling`, `defending`, `physical`, `teamId`, `leagueId`

**`custom_teams`** — user-created teams for the AI Lab  
Fields: `id`, `name`, `formation`, `avgPace`, `avgShooting`, ... , `playerIds` (stored as comma-joined string via TypeConverter), `leagueId`

### Migration 1→2 — Seed data tables

**`real_teams`** — seeded from JSON assets on first launch  
Fields: `id` (AllSports team ID), `name`, `badgeUrl`, `leagueId`, `leagueName`, `country`

**`real_players`** — seeded from JSON assets on first launch  
Fields: `id`, `name`, `position`, `nationality`, `age`, `pace`, `shooting`, `passing`, `dribbling`, `defending`, `physical`, `overall`, `teamId`, `teamName`, `leagueId`, `leagueName`  
The `overall` rating (0–99, FC-style) drives transfer pricing and AI simulation weighting.

### Migration 2→3 — User club tables

**`user_club`** — one row per season the user has played  
Fields: `id`, `clubName`, `leagueId`, `leagueName`, `formation`, `budget`, `seasonYear`, `isActive`  
Only one club has `isActive = 1` at a time. When a new season starts, all clubs are deactivated first.

**`user_squad`** — players signed to the active club  
Fields: `id`, `clubId`, `playerId`, `isStartingXI`, `pitchPosition`  
`pitchPosition` is null until the user assigns the player to a slot in the Squad Builder.

### Migration 3→4 — Season game tables

**`fixture`** — all 380 matches generated at season start  
Fields: `id`, `seasonId` (= `user_club.id`), `matchday` (1–38), `homeTeamId`, `homeTeamName`, `awayTeamId`, `awayTeamName`, `isUserTeam`, `isSimulated`  
The user's club uses `teamId = 0` in this table (real teams use their AllSports IDs). `isUserTeam = 1` flags the 38 fixtures that involve the user.

**`match_result`** — one row per simulated fixture  
Fields: `id`, `fixtureId`, `homeGoals`, `awayGoals`, `matchSummary`

**`goal_scorer`** — one row per goal in a simulated match  
Fields: `id`, `seasonId`, `fixtureId`, `playerName`, `teamName`, `minute`, `isUserTeamPlayer`

**`season_standing`** — 20 rows per season, updated after every matchday  
Fields: `id`, `seasonId`, `teamId`, `teamName`, `played`, `won`, `drawn`, `lost`, `goalsFor`, `goalsAgainst`, `points`, `isUserTeam`

---

## 6. API Integration

### AllSports API (`api/`)

Base URL: `https://apiv2.allsportsapi.com/football/`  
Key injected as `?APIkey=` query param on every request.

Endpoints used:
- `GET /?met=Countries` — list of countries
- `GET /?met=Leagues&countryId=X` — leagues for a country
- `GET /?met=Standings&leagueId=X&seasonId=Y` — league table
- `GET /?met=Teams&leagueId=X&seasonId=Y` — team list
- `GET /?met=Players&teamId=X&seasonId=Y` — players in a team

All responses are parsed with Gson into response model POJOs. Data is never cached to Room — it is fetched live every time the user opens a screen.

### FootballRepository (`repository/FootballRepository.java`)

Single abstraction over all AllSports calls. ViewModels call `FootballRepository` methods which return `LiveData<T>` backed by Retrofit callbacks posting to `MutableLiveData`.

---

## 7. AI Integration (Gemini)

### GeminiClient / GeminiApi (`ai/`)

Base URL: `https://generativelanguage.googleapis.com/v1beta/`  
Endpoint: `POST /models/gemini-1.5-flash:generateContent?key={apiKey}`  
Request body: `GeminiRequest` — wraps the prompt as `contents[0].parts[0].text`.  
Response body: `GeminiResponse` — extracts text from `candidates[0].content.parts[0].text`.

### GeminiRepository (`ai/GeminiRepository.java`)

Two callers:
- `generateSimulation(prompt, apiKey)` — async, returns `LiveData<String>`, used by the AI Lab
- `callSync(prompt, apiKey)` — blocking via `retrofit.execute()`, used by SeasonRepository on a background thread

### AI Lab (`ui/ailab/`)

Users create a custom player or team, then ask Gemini to predict their performance in a real league. The prompt includes the custom entity's attributes and asks Gemini to compare against real league averages. This uses the async `generateSimulation()` call.

### MatchSimulator (`ai/MatchSimulator.java`)

Builds the prompt for season match simulation and parses the response. Used only by `SeasonRepository`.

**`buildPrompt(fixtures, userTeamInfo)`** — sends up to 10 fixtures in one request. For the user's fixture, it includes the full starting XI with player names and individual overall ratings so Gemini can generate realistic scorer names. Instructs Gemini to return **only** a JSON array.

**`buildSimplePrompt(fixtures)`** — stripped-down retry prompt (team names only, no attributes) used if the full prompt fails.

**`parseResponse(raw, expected)`** — finds the first `[` to last `]` in the raw response (ignores any Gemini prose before/after), parses with Gson `TypeToken<List<ParsedMatch>>`, validates the count matches `expected`, clamps goals to 0–7.

### LocalSimulator (`ai/LocalSimulator.java`)

Pure-probability fallback — no API call required. Uses the user team's average overall rating vs a fixed baseline of 78 for all AI teams. Home win probability = `0.45 + (ratingDiff/10) * 0.05`, clamped to 15%–75%. Draw fixed at 25%. Generates realistic scorelines and uses generic surname placeholders for goal scorers.

---

## 8. Seed Data System

### Why seed data?

AllSports API returns statistical data (goals, cards, appearances) — it does **not** provide FC-style attribute ratings (pace, shooting, overall, etc.) that the simulation is built around. So player attributes are seeded from static JSON files bundled in the app.

### JSON structure (`assets/data/`)

Two files: `premierleague.json` and `laliga.json`

Each file has this structure:
```json
{
  "league": { "id": 177, "name": "Premier League", "country": "England", "season": 2024 },
  "teams": [
    {
      "team_id": 85,
      "name": "Manchester City",
      "badge_url": "...",
      "players": [
        {
          "player_id": 1001,
          "name": "Erling Haaland",
          "position": "FW",
          "overall": 91,
          "pace": 89, "shooting": 94, "passing": 65,
          "dribbling": 80, "defending": 45, "physical": 88
        }
      ]
    }
  ]
}
```

Player `overall` (0–99) also determines transfer market price:
- 90+ → €55M–€80M
- 85–89 → €30M–€55M
- 80–84 → €15M–€30M
- 75–79 → €6M–€15M
- 70–74 → €2M–€6M
- Below 70 → €500K–€2M

### SeedLoader (`db/SeedLoader.java`)

Called from `FootMashApp.onCreate()` on a background thread. Checks a `SharedPreferences` flag (`seed_loaded`). If false, reads both JSON files from `assets/data/`, bulk-inserts all teams and players into Room, then sets the flag to true. All subsequent app launches skip this step entirely.

---

## 9. Season Mode — Full Game Flow

The season mode spans 6 implementation phases. The full user journey:

```
LeaguePickerFragment
    → ClubSetupFragment
        → TransferMarketFragment
            → SquadBuilderFragment
                → SeasonHubFragment ←── main loop
                        │
                        ├── MatchdayFragment (simulate each kolo)
                        │
                        └── SeasonSummaryFragment (after kolo 38)
```

---

### Phase 2 — Club & Draft

**LeaguePickerFragment** presents two hardcoded league options (Premier League ID 177, La Liga ID 302) as a RecyclerView. Tapping one navigates to ClubSetup with `leagueId` and `leagueName` as Bundle args.

**ClubSetupFragment** shows a text input for the club name. On confirm, it:
1. Calls `userClubDao.deactivateAll()` — marks any previous active season as inactive
2. Inserts a new `UserClub` with `budget = €100,000,000`, `isActive = true`
3. Navigates to TransferMarket with the new `clubId`

**TransferMarketFragment** loads all `RealPlayer` rows for the selected league from Room. Players are displayed in a RecyclerView (`TransferMarketAdapter`) sorted by overall DESC. Each card shows: overall badge (color-coded gold/silver/bronze/blue), name, position, team, market value, and a "Potpiši"/"Otpusti" toggle button.

Signing a player:
- Deducts the player's `marketValue` from the club's budget
- Inserts a `UserSquad` row linking `clubId` and `playerId`

Releasing a player:
- Refunds the market value
- Deletes the `UserSquad` row

The "Nastavi" button is enabled once the squad reaches 11 players. Navigation passes `clubId` to SquadBuilder.

---

### Phase 3 — Squad Builder

**SquadBuilderViewModel** holds a `LinkedHashMap` of four formations:

| Formation | Slots |
|---|---|
| 4-4-2 | GK + 4 DEF + 4 MID + 2 FWD = 11 |
| 4-3-3 | GK + 4 DEF + 3 MID + 3 FWD = 11 |
| 3-5-2 | GK + 3 DEF + 5 MID + 2 FWD = 11 |
| 4-2-3-1 | GK + 4 DEF + 2 CDM + 3 CAM/W + 1 ST = 11 |

Each slot is a `FormationSlot`: key (e.g. "CB1"), label (e.g. "CB"), posGroup ("DF"), `xPct` and `yPct` (0–1 coordinates on the pitch canvas). The coordinate system has 0,0 at top-left; lower `yPct` = closer to the attacking end.

**Pitch rendering** in `SquadBuilderFragment` uses a `FrameLayout` with a pitch-green background drawable. A `ViewTreeObserver.OnGlobalLayoutListener` fires once the layout is measured. At that point, each `FormationSlot` inflates an `item_pitch_slot.xml` view and positions it with `FrameLayout.LayoutParams`:
```java
left = (int)(slot.xPct * pitchWidth)  - slotWidth/2
top  = (int)(slot.yPct * pitchHeight) - slotHeight/2
```
Both values are clamped to stay within the pitch bounds.

**Player assignment** — tapping a slot opens an `AlertDialog` listing all signed players whose position matches the slot's `posGroup` (GK→GK, DF→CB/RB/LB, MF→CM/CDM/CAM/LM/RM, FW→ST/FW/LW/RW). Before assigning, the ViewModel removes any existing assignment for that player (no player in two slots). If the slot is already filled, a "Ukloni" neutral button appears to unassign.

**Saving** — on confirm, `SquadBuilderViewModel.saveLineup()` runs on a background thread:
1. Updates `UserClub.formation`
2. Calls `resetStartingXI(clubId)` — clears all `isStartingXI` and `pitchPosition` fields
3. For each assigned slot, sets `isStartingXI = true` and `pitchPosition = slot.key`

---

### Phase 4 — Season Engine

**Fixture generation** (`SeasonRepository.generateFixtures`) uses the **circle method** round-robin algorithm:

- 20 teams (user club at index 0, 19 real teams at indices 1–19)
- Fix team[0]; create a rotating list ("circle") of indices 1–19
- Each of 19 rounds: pair (fixed, circle[0]) + pair (circle[k], circle[n-1-k]) for k=1..9 → 10 matches per round
- Rotate the circle by moving index 0 to the end
- This produces 190 first-leg fixtures (matchdays 1–19)
- Duplicate the list with home/away swapped for matchdays 20–38
- Total: **380 fixtures**

All fixtures are inserted into Room in one `insertAll()` call when `startSeasonIfNeeded()` detects `countFixtures(clubId) == 0`.

**Standings** are initialised at 0 for all 20 teams simultaneously. After each simulated matchday, `recalculateStandings()` resets all rows to 0 and rebuilds from every simulated fixture result from scratch — this avoids incremental accumulation bugs.

**SeasonHubFragment** is the persistent dashboard. It shows:
- Club header (name, league, remaining budget)
- "Next match" card: home vs away team for the upcoming user fixture
- "Simulate" button: label updates to "Simuliraj kolo N" as matchdays progress, or "Pogledaj završnicu" when all 38 are done
- Live standings table (20 rows, user team row highlighted in translucent accent blue)

The fragment uses three separate LiveData sources from Room: `getNextUserFixture()`, `getNextMatchdayLive()`, and `getStandings()`.

---

### Phase 5 — AI Match Simulation

Tapping "Simuliraj kolo N" on SeasonHub navigates to `MatchdayFragment` with `clubId` and `matchday` as args.

**MatchdayFragment** shows all 10 fixtures for that matchday in a RecyclerView. Before simulation, each card shows "Home vs Away" with a muted separator. The "Simuliraj kolo" button triggers `MatchdayViewModel.simulate()`.

**Simulation flow** (runs entirely on a background thread):

```
SeasonRepository.simulateMatchday(clubId, matchday, apiKey)
    │
    ├── Load fixtures from DB
    ├── Load user team starting XI + compute avg overall
    │
    ├── Try: GeminiRepository.callSync(buildPrompt(...))
    │       → parseResponse() → if valid list of 10: use it
    │
    ├── Retry: GeminiRepository.callSync(buildSimplePrompt(...))
    │       → parseResponse() → if valid list of 10: use it
    │
    └── Fallback: LocalSimulator.simulateAll(...)
            → always succeeds, uses probability model
    │
    └── saveResults(): insert MatchResult + GoalScorer rows, markSimulated()
    └── recalculateStandings(): rebuild all 20 standing rows from scratch
```

**Simulation state** (`MatchdayViewModel.SimState`): `IDLE → SIMULATING → DONE / ERROR`  
- `SIMULATING`: ProgressBar visible, button disabled
- `DONE`: button text changes to "Kolo odigrano", button locked
- `ERROR`: toast shown, button re-enabled for retry

After simulation, `MatchdayAdapter` re-binds each card to show the score ("2 — 1"), goal scorers aligned under home/away columns with minute notation ("Salah 23', Salah 67'"). The user's team name is highlighted in accent blue.

**Scorers text encoding** — the ViewModel encodes scorers as a pipe-delimited string `"homeScorers|awayScorers"`. The adapter splits on `|` and sets each half into the respective `TextView`.

---

### Phase 6 — Season End

**End detection**: `getNextMatchdayLive(clubId)` returns `null` / `0` when `SELECT MIN(matchday) FROM fixture WHERE isSimulated = 0` has no rows (all 380 fixtures are done). `SeasonHubFragment.bindMatchday()` detects this and changes the button to "Pogledaj završnicu".

**SeasonSummaryFragment** receives `clubId` and loads:
- **Champion** — `getStandingsSync()[0]` (sorted by points → goal diff → goals for)
- **Golden Boot** — `getTopScorers()` — SQL aggregate: `SELECT playerName, teamName, COUNT(*) AS goals FROM goal_scorer GROUP BY playerName, teamName ORDER BY goals DESC LIMIT 20`
- **User result** — scans sorted standings for `isUserTeam = true`, records position index

The summary displays three cards:
1. **Champion card** — gold stroke border, team name, points total
2. **Golden Boot card** — player name, team, goals count
3. **User Result card** — blue stroke border, position badge, W/D/L record, goals F:A, points, contextual message:
   - Position 1 → "Prvak si! Čestitke!"
   - Position 2–4 → "Odlično! Europska mjesta!"
   - Position 5–10 → "Solidna sezona!"
   - Position 11–17 → "Preživjeli ste!"
   - Position 18–20 → "Ispali ste!"

**"Nova sezona"** — navigates to `nav_season` (LeaguePickerFragment) using `NavOptions.popUpTo(R.id.nav_season, inclusive=true)`. This pops `nav_season` itself off the back stack and then re-adds it fresh, clearing all intermediate screens (ClubSetup, TransferMarket, SquadBuilder, SeasonHub, SeasonSummary). The next `ClubSetupFragment` will call `deactivateAll()` before inserting the new club.

---

## 10. Navigation Graph

All navigation is in a single `nav_graph.xml`. The start destination is `nav_home`.

```
nav_home
  └──(action_home_to_countries)──► nav_countries
                                        └──► nav_leagues_list
                                                ├──► nav_standings
                                                └──► nav_teams_list
                                                          └──► nav_team_detail
                                                                    └──► nav_players
                                                                              └──► nav_player_detail

nav_leagues (bottom nav tab — same as nav_countries)
  └──► nav_leagues_list ──► ...

nav_teams (bottom nav tab — shows top teams)
  └──► nav_team_detail ──► nav_players ──► nav_player_detail

nav_ai_lab (bottom nav tab)
  ├──► playerSimulation
  └──► teamSimulation

nav_season (bottom nav tab — LeaguePickerFragment)
  └──► nav_club_setup
          └──► nav_transfer_market
                    └──► nav_squad_builder
                                └──► nav_season_hub  ◄── season main loop
                                          ├──► nav_matchday
                                          └──► nav_season_summary
```

**Bottom nav visibility** is controlled in `MainActivity`: the bar is shown only on the five top-level tab destinations (`nav_home`, `nav_leagues`, `nav_teams`, `nav_ai_lab`, `nav_season`, `nav_season_hub`). All sub-screens hide it.

---

## 11. Key Patterns & Conventions

### MVVM with LiveData

Every Fragment has a matching ViewModel. Fragments never call Room or Retrofit directly. All DB writes happen on background threads (via `new Thread(() → { ... }).start()`). LiveData results are posted with `postValue()` from background threads.

### ViewBinding

Every Fragment follows this pattern:
```java
private FragmentXxxBinding binding;
// onCreateView:  binding = FragmentXxxBinding.inflate(...)
// onDestroyView: binding = null   ← prevents memory leaks
```

### Room background threading

Room does not allow database access on the main thread. Any sync DAO method (`*Sync`) must be called from a background thread. LiveData-returning DAO methods are observed on the main thread — Room handles their internal threading automatically.

### Navigation with Bundle args

Fragments receive data via `getArguments().getInt("key")` / `getString("key")`. Args are declared in nav_graph.xml with `app:argType`. This is the standard safe-args pattern (without the safe-args plugin — raw Bundle is used instead).

### UI theme

Background: `#080A12` (near-black dark blue)  
Cards: `#1A1D24` (dark card surface)  
Accent: `#2962FF` (Material Blue 700)  
Text: `#FFFFFF` primary, `#88FFFFFF` secondary (50% alpha)  
User team highlight: `0x332962FF` (20% opaque accent blue)  
Champion highlight: `#FFD700` (gold)

---

## 12. File Reference

### Season Mode files — quick lookup

| File | Phase | Purpose |
|---|---|---|
| `model/FormationSlot.java` | 3 | Pitch position descriptor (key, label, posGroup, xPct, yPct) |
| `ui/season/SquadBuilderViewModel.java` | 3 | Formation map, slot assignments, saveLineup() |
| `ui/season/SquadBuilderFragment.java` | 3 | Pitch canvas rendering, slot tap → player picker dialog |
| `model/Fixture.java` | 4 | Room entity: scheduled match |
| `model/MatchResult.java` | 4 | Room entity: simulated match score |
| `model/GoalScorer.java` | 4 | Room entity: individual goal event |
| `model/SeasonStanding.java` | 4 | Room entity: live league table row |
| `db/FixtureDao.java` | 4/5 | All fixture/result/scorer queries |
| `db/StandingDao.java` | 4 | Standing queries + top scorer aggregate |
| `repository/SeasonRepository.java` | 4/5 | startSeasonIfNeeded(), simulateMatchday(), recalculateStandings() |
| `ui/season/SeasonHubViewModel.java` | 4 | Standings + next fixture + next matchday LiveData |
| `ui/season/SeasonHubFragment.java` | 4/5/6 | Season dashboard, routes to Matchday or Summary |
| `ai/MatchSimulator.java` | 5 | Gemini prompt builder + JSON parser (ParsedMatch, Scorer, UserTeamInfo) |
| `ai/LocalSimulator.java` | 5 | Probability fallback simulator |
| `ui/season/MatchdayViewModel.java` | 5 | SimState machine, simulate(), buildDisplays() |
| `ui/season/MatchdayAdapter.java` | 5 | Fixture cards pre/post simulation |
| `ui/season/MatchdayFragment.java` | 5 | Simulate button, progress bar, results list |
| `ui/season/SeasonSummaryViewModel.java` | 6 | Loads champion, golden boot, user final stats |
| `ui/season/SeasonSummaryFragment.java` | 6 | End-of-season display, "Nova sezona" navigation |

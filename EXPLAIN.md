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
9. [Player Traits & Synergy System](#9-player-traits--synergy-system)
10. [Season Mode — Full Game Flow](#10-season-mode--full-game-flow)
11. [Navigation Graph](#11-navigation-graph)
12. [Key Patterns & Conventions](#12-key-patterns--conventions)
13. [File Reference](#13-file-reference)

---

## 1. Project Purpose

FootMash is an Android football (soccer) manager simulation game. It combines two distinct experiences:

**Browse Mode** — live data from the AllSports API lets users browse real leagues, teams, players, and standings.

**Season Mode** — the core game. The user picks one of five seeded leagues, either founds a brand-new club with a €100M transfer budget or inherits an existing real club (pre-stocked squad, €50M budget), drafts a starting XI directly onto a formation pitch, then simulates an entire 38-matchday season using Google Gemini AI to generate realistic scorelines and goal scorers. A full league table, top scorers chart, and end-of-season summary are produced.

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
| AI | Google Gemini 2.0 Flash | via REST API |

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

**ViewBinding** is used throughout — no `findViewById` in the fragment layer (a couple of inline RecyclerView ViewHolders still use it for item views).

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
│   ├── GeminiApi.java            — Retrofit interface (gemini-2.0-flash generateContent)
│   ├── GeminiRequest.java        — Request body POJO (contents → parts → text)
│   ├── GeminiResponse.java       — Response body POJO (candidates → content → parts)
│   ├── GeminiRepository.java     — Async (LiveData) + sync (blocking) Gemini callers
│   ├── MatchSimulator.java       — Prompt builder + JSON response parser for match sim
│   └── LocalSimulator.java       — Probability-based fallback simulator (no API needed)
│
├── model/
│   │   — API response models (read-only, never stored in Room)
│   ├── LeagueResponse.java
│   ├── LeagueInfo.java
│   ├── TeamResponse.java
│   ├── PlayerResponse.java
│   │
│   │   — AI Lab models (Room entities, user-created)
│   ├── CustomPlayer.java
│   ├── CustomTeam.java
│   │
│   │   — Seed data models (Room entities, loaded from JSON assets)
│   ├── RealPlayer.java           — getMarketValue() pricing + formDelta / getEffectiveOverall()
│   ├── RealTeam.java
│   │
│   │   — Season Mode models (Room entities, created during gameplay)
│   ├── UserClub.java             — STARTING_BUDGET / HALF_BUDGET constants
│   ├── UserSquad.java
│   ├── FormationSlot.java        — pitch position descriptor (not a Room entity)
│   ├── Fixture.java
│   ├── MatchResult.java
│   ├── GoalScorer.java
│   ├── SeasonStanding.java
│   │
│   │   — Trait system (plain objects, never stored in Room)
│   ├── Trait.java                — enum of 19 playstyle traits, 4–5 per position group
│   └── SynergyResult.java        — rating + delta + active-combo breakdown
│
├── season/
│   ├── FormationCatalog.java     — the 4 formations, their pitch slots, position-group matching
│   └── TraitEngine.java          — derives traits from attributes, evaluates squad synergy
│
├── db/
│   ├── AppDatabase.java          — Room database singleton, version 6, all migrations
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
│   ├── DraftRepository.java      — Pre-seeds the starting XI for an inherited real club
│   └── SeasonRepository.java     — Season game logic (fixture gen, simulation, standings)
│
└── ui/
    ├── MainActivity.java
    │
    ├── home/        — HomeFragment + HomeViewModel
    ├── leagues/     — LeaguesFragment + LeaguesAdapter + LeaguesViewModel
    ├── teams/       — TeamsFragment + TeamsAdapter + TeamsViewModel + TeamDetailFragment
    ├── players/     — PlayersFragment + PlayersAdapter + PlayersViewModel + PlayerDetailFragment
    │
    ├── ailab/
    │   ├── AILabFragment.java
    │   ├── PlayerSimulationFragment.java + PlayerSimulationViewModel.java
    │   ├── TeamSimulationFragment.java + TeamSimulationViewModel.java
    │   └── SearchAdapter.java
    │
    ├── util/
    │   ├── TeamBadges.java            — team_id → bundled crest drawable lookup (+ fallback)
    │   ├── ClubColors.java            — per-club colour themes for Season Mode (100 clubs)
    │   └── InitialsBadgeDrawable.java — generated initials badge when no crest is bundled
    │
    └── season/
        ├── LeaguePickerFragment.java     — pick one of 5 seeded leagues
        ├── ClubModeFragment.java         — choose "new club" or "existing club"
        ├── ClubSetupFragment.java        — (new club) name the club
        ├── FormationPickerFragment.java  — (new club) pick a formation
        ├── ExistingClubPickerFragment.java — (existing club) pick a real team to inherit
        ├── DraftCanvasFragment.java      — pitch canvas: fill slots from a slot-targeted market
        ├── DraftViewModel.java           — draft state: assignments, budget, market candidates
        ├── PlayerMarketBottomSheet.java  — modal market for the currently-selected slot
        ├── SeasonHubFragment.java        — season dashboard
        ├── SeasonHubViewModel.java       — standings, next fixture, next matchday
        ├── MatchdayFragment.java         — simulate button + results list
        ├── MatchdayAdapter.java          — fixture cards pre/post simulation
        ├── MatchdayViewModel.java        — simulation state + fixture display
        ├── SeasonSummaryFragment.java    — end-of-season screen
        └── SeasonSummaryViewModel.java   — champion, golden boot, user result
```

---

## 5. Database Schema

The Room database (`footmash_database`) is at **version 6** with five migrations. All tables are listed below.

### Pre-existing tables (version 1)

**`custom_players`** — user-created players for the AI Lab
Fields: `id`, `firstName`, `lastName`, `age`, `position`, `pace`, `shooting`, `passing`, `dribbling`, `defending`, `physical`, `teamId`, `leagueId`

**`custom_teams`** — user-created teams for the AI Lab
Fields: `id`, `name`, `formation`, `avgPace`, `avgShooting`, ... , `playerIds` (stored as comma-joined string via TypeConverter), `leagueId`

### Migration 1→2 — Seed data tables

**`real_teams`** — seeded from JSON assets on first launch
Fields: `id` (seed team ID), `name`, `badgeUrl`, `leagueId`, `leagueName`, `country`

**`real_players`** — seeded from JSON assets on first launch
Fields: `id`, `name`, `position`, `nationality`, `age`, `pace`, `shooting`, `passing`, `dribbling`, `defending`, `physical`, `overall`, `teamId`, `teamName`, `leagueId`, `leagueName`, `formDelta`
The `overall` rating (0–99, FC-style) drives transfer pricing and AI simulation weighting. `formDelta` (added in v7) carries in-season form — see [Dynamic Player Growth](#10-season-mode--full-game-flow).

### Migration 2→3 — User club tables

**`user_club`** — one row per season the user has played
Fields: `id`, `clubName`, `leagueId`, `leagueName`, `formation`, `budget`, `seasonYear`, `isActive`
Only one club has `isActive = 1` at a time. When a new season starts, all clubs are deactivated first.

**`user_squad`** — players signed to the active club
Fields: `id`, `clubId`, `playerId`, `isStartingXI`, `pitchPosition`
`pitchPosition` is the formation slot key (e.g. `CB1`, `LW`) the player occupies on the draft canvas.

### Migration 3→4 — Season game tables

**`fixture`** — all 380 matches generated at season start
Fields: `id`, `seasonId` (= `user_club.id`), `matchday` (1–38), `homeTeamId`, `homeTeamName`, `awayTeamId`, `awayTeamName`, `isUserTeam`, `isSimulated`
In this table the user's club uses `teamId = 0` (real teams use their seed IDs). `isUserTeam = 1` flags the 38 fixtures involving the user.

**`match_result`** — one row per simulated fixture
Fields: `id`, `fixtureId`, `homeGoals`, `awayGoals`, `matchSummary`

**`goal_scorer`** — one row per goal in a simulated match
Fields: `id`, `seasonId`, `fixtureId`, `playerName`, `teamName`, `minute`, `isUserTeamPlayer`

**`season_standing`** — 20 rows per season, updated after every matchday
Fields: `id`, `seasonId`, `teamId`, `teamName`, `played`, `won`, `drawn`, `lost`, `goalsFor`, `goalsAgainst`, `points`, `isUserTeam`, `badgeUrl`

### Migration 4→5 — Inherited clubs

Adds `realTeamSourceId` (nullable `INTEGER`) to `user_club`. When the user inherits an existing real club, this stores that club's `real_teams.id` — used later to remove that team from the fixture list so the user isn't scheduled against themselves.

### Migration 5→6 — Standing crests

Adds `badgeUrl` (`TEXT`) to `season_standing` so the live league table can render team crests.

### Migration 6→7 — Dynamic player form

Adds `formDelta` (`INTEGER NOT NULL DEFAULT 0`) to `real_players`. It holds in-season rating growth/decline; a player's effective rating is `overall + formDelta` (see [Dynamic Player Growth](#10-season-mode--full-game-flow) in section 10).

---

## 6. API Integration

### AllSports API (`api/`)

Base URL: `https://apiv2.allsportsapi.com/football/`
Key injected as `?APIkey=` query param on every request. Powers the live Browse Mode screens (leagues, teams, players, standings). Data is fetched live and is not cached to Room.

### FootballRepository (`repository/FootballRepository.java`)

Single abstraction over all AllSports calls. ViewModels call `FootballRepository` methods which return `LiveData<T>` backed by Retrofit callbacks posting to `MutableLiveData`.

---

## 7. AI Integration (Gemini)

### GeminiClient / GeminiApi (`ai/`)

Base URL: `https://generativelanguage.googleapis.com/v1beta/`
Endpoint: `POST /models/gemini-2.0-flash:generateContent?key={apiKey}`
(Gemini 1.5 Flash was retired; the app now targets 2.0 Flash.)
Request body: `GeminiRequest` — wraps the prompt as `contents[0].parts[0].text`.
Response body: `GeminiResponse` — extracts text from `candidates[0].content.parts[0].text`.

### GeminiRepository (`ai/GeminiRepository.java`)

Two callers:
- `generateSimulation(prompt, apiKey)` — async, returns `LiveData<String>`, used by the AI Lab
- `callSync(prompt, apiKey)` — blocking via `retrofit.execute()`, used by SeasonRepository on a background thread

### AI Lab (`ui/ailab/`)

Users create a custom player or team, then ask Gemini to predict their performance in a real league. Uses the async `generateSimulation()` call.

### MatchSimulator (`ai/MatchSimulator.java`)

Builds the prompt for season match simulation and parses the response. Used only by `SeasonRepository`.

**`buildPrompt(fixtures, userTeamInfo, ratings)`** — sends up to 10 fixtures in one request. Every match line states each team's **effective rating** (`ratings` map — see [section 9](#9-player-traits--synergy-system)). For the user's fixture it also includes the full starting XI with names, overall ratings, and each player's traits, plus the squad's synergy delta, so Gemini can generate realistic scorer names and rating-aware results. Instructs Gemini to return **only** a JSON array.

**`buildSimplePrompt(fixtures, ratings)`** — stripped-down retry prompt (team names + effective ratings only) used if the full prompt fails.

**`parseResponse(raw, expected)`** — finds the first `[` to last `]` in the raw response, parses with Gson `TypeToken<List<ParsedMatch>>`, validates the count matches `expected`, clamps goals to 0–7.

### LocalSimulator (`ai/LocalSimulator.java`)

Pure-probability fallback — no API call required. Takes a team-name → effective-rating map and weights each match's win probability by the rating gap (teams not in the map fall back to a baseline of 78). Generates realistic scorelines and uses generic surname placeholders for goal scorers.

---

## 8. Seed Data System

### Why seed data?

AllSports API returns statistical data (goals, cards, appearances) — it does **not** provide FC-style attribute ratings (pace, shooting, overall, etc.) that the simulation is built around. So player attributes are seeded from static JSON files bundled in the app.

### JSON files (`assets/data/`)

Five league files, each holding the **2025/26** season squads:

| File | League | League ID | Teams | Players |
|---|---|---|---|---|
| `premierleague.json` | Premier League | 177 | 20 | ~400 |
| `laliga.json` | La Liga | 302 | 20 | ~358 |
| `bundesliga.json` | Bundesliga | 78 | 20 | 400 |
| `seriea.json` | Serie A | 207 | 20 | 400 |
| `ligue1.json` | Ligue 1 | 168 | 20 | 400 |

Every file has 20 teams — required because the season engine's round-robin only produces a complete schedule for an even total team count (the user's club + 19 opponents). Bundesliga and Ligue 1 (18 clubs in reality) are padded with two notable second-tier clubs each.

Each file has this structure:
```json
{
  "league": { "id": 78, "name": "Bundesliga", "country": "Germany", "season": 2025 },
  "teams": [
    {
      "team_id": 701,
      "name": "Bayern Munich",
      "badge_url": "",
      "players": [
        {
          "player_id": 7001,
          "name": "Harry Kane",
          "position": "ST",
          "nationality": "England",
          "age": 32,
          "overall": 90,
          "pace": 72, "shooting": 93, "passing": 84,
          "dribbling": 83, "defending": 47, "physical": 86
        }
      ]
    }
  ]
}
```

`player_id` and `team_id` ranges are kept disjoint per file to avoid primary-key collisions in `real_players` / `real_teams`.

### Transfer pricing (`RealPlayer.getMarketValue()`)

Market value is a flat tier derived from `overall`:

| Overall | Market Value |
|---|---|
| 90+ | €70M |
| 85–89 | €42M |
| 80–84 | €22M |
| 75–79 | €10M |
| 70–74 | €4M |
| Below 70 | €1M |

### SeedLoader (`db/SeedLoader.java`)

Called from `FootMashApp.onCreate()` on a background thread. Checks a versioned `SharedPreferences` flag (`KEY_SEED_LOADED`, currently `seed_loaded_v5_2025_26`). If the flag is unset, it wipes `real_players` / `real_teams`, reads all five JSON files from `assets/data/`, bulk-inserts every team and player, then sets the flag. Bumping the key string forces existing installs to reload on next launch — this is how new/changed seed data is shipped.

If a team's `badge_url` is empty, SeedLoader synthesises an AllSports logo URL from the team ID and a kebab-cased name; when that URL has no image, the UI falls back to a generated `InitialsBadgeDrawable`.

---

## 9. Player Traits & Synergy System

Every player carries **up to 3 playstyle traits** (e.g. Goal Poacher, Curved Crosser, Ball-Playing Defender). Traits are **auto-derived** from a player's position and six attributes — they are never stored in the database and require no seed-file edits. Their value comes from how they **combine across a starting XI**: good pairings raise a team's strength, clashing ones lower it. The whole system is pure logic in `season/TraitEngine.java`, backed by `model/Trait.java` and `model/SynergyResult.java`.

### Trait catalog (19 traits)

Each trait belongs to one position group; a player can only earn traits from their own group's pool.

| Group | Traits |
|---|---|
| **GK** | Shot Stopper · Sweeper Keeper · Playmaker GK · Aerial Commander |
| **DF** | Ball-Playing Defender · No-Nonsense Defender · Overlapping Full-Back · Aerial Wall · Last-Ditch Tackler |
| **MF** | Playmaker · Box-to-Box · Curved Crosser · Ball Winner · Tempo Setter |
| **FW** | Goal Poacher · False 9 · Target Man · Speed Merchant · Clinical Finisher |

(Display names are Croatian — e.g. *Lovac na golove*, *Majstor centra*. See `Trait.java`.)

### Derivation (`TraitEngine.deriveTraits`)

For each trait in the player's position group a **fit score** is computed from the relevant attributes (e.g. Goal Poacher = shooting, penalised by high passing; Curved Crosser = passing + pace). The player keeps the top traits scoring ≥ 62, capped at 3, and always keeps at least their single strongest trait. Goalkeepers are a special case — the six attributes don't model reflexes, so the core Shot Stopper trait keys off `overall` instead.

### Synergy (`TraitEngine.computeSynergy`)

Every pair of players in the XI is checked against a synergy table. Matching trait pairs add or subtract weight:

- **Positive (++ / +)** — e.g. Curved Crosser → Goal Poacher, Playmaker → Speed Merchant, Ball-Playing Defender → Playmaker, Playmaker GK → Speed Merchant (counter-attack)
- **Negative (− / −−)** — e.g. Goal Poacher + False 9 (clash for the same central space), Sweeper Keeper + No-Nonsense Defender (high line vs deep block)

The summed weight maps to a `SynergyResult`:
- `rating` — 0–100 display score (≈50 neutral)
- `delta` — clamped to **−5…+6**, added to the team's effective rating
- `positives` / `negatives` — the active-combo descriptions shown in the draft breakdown dialog

### Effective rating

A team's **effective rating = average overall of its XI + synergy delta**. This single number drives simulation:

- **User club** — synergy is computed over the actual drafted starting XI.
- **AI opponents** — `TraitEngine.bestXi()` picks each club's strongest 11 (1 GK / 4 DF / 3 MF / 3 FW), then synergy is computed over that. So **every team in the league gets synergy**, not just the user — your synergy is an edge, not a free win.

`SeasonRepository` builds a team-name → effective-rating map each matchday and feeds it to the Gemini prompt and the local fallback (see [section 10](#10-season-mode--full-game-flow)). The delta is deliberately small so synergy *tilts* close games rather than deciding them — overall rating still dominates.

### Where traits surface in the UI

- **Player market bottom sheet** — each player card shows up to two colour-coded trait chips (gold GK / blue DF / green MF / red FW).
- **Draft canvas** — a live **⚡ Synergy card** shows the current XI's rating /100 and its `+/−` OVR delta, recomputed every time a player is bought or sold. Tapping it opens a breakdown dialog listing which good and bad combinations are firing.

### Tests

`app/src/test/java/hr/fipu/footmash/TraitEngineTest.java` — JUnit coverage for derivation, the 3-trait cap, synergy sign, best-XI selection, and effective rating (pure logic, no Android).

---

## 10. Season Mode — Full Game Flow

The full user journey:

```
LeaguePickerFragment            pick 1 of 5 leagues
    → ClubModeFragment          "new club"  or  "existing club"
        │
        ├─ NEW CLUB:   ClubSetupFragment  →  FormationPickerFragment  ┐
        │                                                            ├→ DraftCanvasFragment
        └─ EXISTING:   ExistingClubPickerFragment  ───────────────────┘
                                                                          │
                                                              SeasonHubFragment  ←── main loop
                                                                    ├── MatchdayFragment
                                                                    └── SeasonSummaryFragment
```

### League selection

**LeaguePickerFragment** lists the five seeded leagues (Premier League, La Liga, Bundesliga, Serie A, Ligue 1) as a RecyclerView. Selecting one passes `leagueId` + `leagueName` to ClubMode.

### Club mode

**ClubModeFragment** offers two cards:
- **New club** → `ClubSetupFragment`
- **Existing club** → `ExistingClubPickerFragment`

### Path A — New club

**ClubSetupFragment** takes a club name. On confirm it runs on a background thread:
1. `userClubDao.deactivateAll()` — marks any previous season inactive
2. Inserts a new `UserClub` with `budget = STARTING_BUDGET` (€100M), `formation = "4-3-3"`, `isActive = true`
3. Navigates to FormationPicker with the new `clubId`

**FormationPickerFragment** lists the four formations from `FormationCatalog` (4-4-2, 4-3-3, 3-5-2, 4-2-3-1) each with a short Croatian description. The chosen formation is written to `UserClub.formation`, then navigates to DraftCanvas.

### Path B — Existing club

**ExistingClubPickerFragment** lists every real team in the chosen league (`real_teams`). Selecting one inherits it:
1. `deactivateAll()`
2. Inserts a `UserClub` named after the team, `budget = HALF_BUDGET` (€50M), `formation = "4-3-3"`, `realTeamSourceId = team.id`
3. `DraftRepository.seedExistingClub(...)` pulls the real team's roster, sorts by `overall` DESC, and best-fits the strongest players into the formation slots by position group, inserting them into `user_squad` as starting-XI rows
4. Navigates straight to DraftCanvas — already pre-stocked

### Draft — the pitch canvas

**DraftCanvasFragment** + **DraftViewModel** are the heart of the draft. The fragment renders the chosen formation as positioned slot views on a pitch-green `FrameLayout` (using each `FormationSlot`'s `xPct`/`yPct` coordinates, clamped to the pitch bounds, measured via a `ViewTreeObserver` global-layout listener).

`DraftViewModel` (activity-scoped, fully reset by `init()`) holds:
- `formation` — the active formation key
- `assignments` — `Map<slotKey, RealPlayer>` of who occupies each pitch slot
- `bench` — `List<RealPlayer>` of substitutes (up to `MAX_BENCH` = 7)
- `remainingBudget` — live budget in euros
- `selectedSlotKey` — the slot the market modal is buying for (or the `BENCH_KEY` sentinel)
- `marketCandidates` — a `MediatorLiveData` recomputed whenever league players, assignments, bench, the selected slot, or the budget change

**Interacting with the pitch (draft):**
- **Tap an empty slot** → choose an eligible bench player to slot in, or "Kupi s tržišta" → `PlayerMarketBottomSheet`. The market shows league players that match the slot's position group, aren't already owned (XI or bench), and cost ≤ the remaining budget. Buying calls `buyForSlot()`.
- **Tap a filled slot** → *Na klupu* (`moveToBench()`) or *Prodaj* (`sellFromSlot()` — refunds and deletes the `user_squad` row).
- **"+ Dodaj"** → `selectBench()` opens the market with no position filter; `buyForBench()` adds a substitute (`isStartingXI = false`).
- **Formation chips** above the pitch switch formation at any time — `changeFormation()` best-fits the current starters into the new shape and drops any overflow to the bench.

The market modal (`PlayerMarketBottomSheet`) has a **name search field** and a **position filter** (Svi / GK / DF / MF / FW). Filtering is applied client-side over the candidate list from `DraftViewModel.getMarketCandidates()`; the position filter row is shown only in bench mode, where the market spans every position.

**Lineup validation** (`validateLineup()`) blocks "Start season" until: every slot is filled, the budget is not negative, exactly one goalkeeper is present, and no player is double-assigned. The fragment surfaces any failed reasons in a dialog.

On a valid lineup, "Start season" navigates to SeasonHub with `clubId`.

### Season Engine

`SeasonRepository.startSeasonIfNeeded(clubId)` runs once when SeasonHub first loads (guarded by `countFixtures(clubId) == 0`):

1. Loads all real teams for the league. If the club has a `realTeamSourceId`, that team is removed from the pool (so the user doesn't face their own inherited club).
2. Takes the first 19 remaining teams → `1 user club + 19 real = 20` teams.
3. **Fixture generation** uses the **circle method** round-robin: fix the user team at index 0, rotate the other 19, producing 19 rounds × 10 matches = 190 first-leg fixtures, then duplicates them with home/away swapped for matchdays 20–38. **380 fixtures total.**
4. Initialises 20 `season_standing` rows at 0.

> The round-robin is correct only for an **even** total team count, which is why every seed file ships exactly 20 teams.

**SeasonHubFragment** is the persistent dashboard: club header (name, league, budget), a "next match" card, a "Simuliraj kolo N" button (or "Pogledaj završnicu" once all 38 matchdays are done), and the live 20-row standings table with the user's row highlighted.

### AI Match Simulation

Tapping "Simuliraj kolo N" opens **MatchdayFragment** (`clubId` + `matchday` args), showing that matchday's 10 fixtures. The "Simuliraj kolo" button runs `SeasonRepository.simulateMatchday()` on a background thread:

```
Load the 10 fixtures + the user XI
Build effective-rating map (avg overall + trait synergy) for every team — see section 9
    │
    ├── Try:    callSync(buildPrompt(..., ratings))      → parseResponse() → if 10 valid: use
    ├── Retry:  callSync(buildSimplePrompt(..., ratings)) → parseResponse() → if 10 valid: use
    └── Fallback: LocalSimulator.simulateAll(fixtures, ratings, rosters) → always succeeds
    │
    └── saveResults(): insert MatchResult + GoalScorer rows, mark fixtures simulated
    └── recalculateStandings(): rebuild all 20 standing rows from scratch
```

`MatchdayViewModel.SimState` runs `IDLE → SIMULATING → DONE / ERROR`. After simulation the cards re-bind to show the score and goal scorers with minute notation. Standings are always recomputed from every simulated result from scratch to avoid incremental-accumulation bugs.

### Whole-Season Simulation

Beside the per-matchday "Simuliraj kolo" button, the Season Hub has a **"Simuliraj sezonu"** button that plays out every remaining matchday in one go. Tapping it opens a themed confirmation dialog, then `SeasonHubViewModel.simulateWholeSeason(apiKey)` runs on a background thread:

- It loops `SeasonRepository.simulateMatchday()` for each remaining matchday (`getNextMatchdaySync` until it returns 0), with a safety guard of 60 iterations.
- Progress is exposed via two LiveData: a `SeasonSimState` (`IDLE → RUNNING → DONE`) and a progress string ("Simuliram kolo X / 38").
- While running, a progress bar shows, the buttons disable, and the standings table fills in live (Room LiveData).
- On `DONE` the fragment navigates straight to `SeasonSummaryFragment` — the next-match card hides itself once no fixtures remain, so auto-navigation avoids a dead end.

Because each matchday is a Gemini call, a full run is dozens of sequential requests; the local fallback guarantees it always completes.

### Season End

When `SELECT MIN(matchday) FROM fixture WHERE isSimulated = 0` has no rows, the SeasonHub button switches to "Pogledaj završnicu".

**SeasonSummaryFragment** shows three cards:
1. **Champion** — `season_standing` sorted by points → goal diff → goals for, row 0
2. **Golden Boot** — `goal_scorer` aggregate: `GROUP BY playerName, teamName ORDER BY COUNT(*) DESC`
3. **User Result** — the user's standing row, position, W/D/L, goals, and a contextual message keyed to final position

The Season Summary offers two paths:

- **"Sljedeća sezona"** — continues the career with the *same* club. `SeasonRepository.startNextSeason()` wipes the finished season's fixtures, results, goal scorers and standings (via delete-by-season DAO queries), advances `UserClub.seasonYear`, and regenerates a fresh 38-matchday fixture list. The squad, formation and budget all carry over; the user lands back on a fresh Season Hub at matchday 1. Player `formDelta` is reset, so each season starts at neutral form.
- **"Nova momčad"** — starts over with a brand-new club: pops back to `nav_season` (LeaguePicker), clearing the season back stack.

So the game never "ends" — it loops season → summary → next season indefinitely.

### Season Year

Each `UserClub` carries a `seasonYear` (new clubs start at **2025**, matching the seed data). `UserClub.getSeasonLabel()` formats it as e.g. `"2025/26"`. The label is shown in the Season Hub header (`League · 2025/26`) and the Season Summary header, and ticks up each time the user continues to the next season.

### Per-Club Theming

When the user **inherits an existing real club**, the Season Mode screens are re-skinned with that club's colours — e.g. picking Real Madrid turns the buttons and league-table highlight white-on-black with gold accents.

**`ui/util/ClubColors.java`** holds a colour catalogue for all 100 seeded clubs, keyed by seed `team_id` (the immutable `real_teams` primary key). Each entry is a `Theme`:

- `primary` — the club's vivid brand colour, used for buttons and headline highlights. Dark kits are represented by a brighter shade so they stay visible on the near-black app background.
- `onPrimary` — a contrasting text/icon colour.
- `accent` — the club's secondary colour, used for card strokes and `rowTint()` (a translucent fill for the user's league-table row).

`ClubColors.of(teamId)` returns the matching theme, or `ClubColors.DEFAULT` (Material Blue) for self-founded clubs — which have no `realTeamSourceId` — and unknown ids.

Each themed fragment loads its `UserClub`, resolves `ClubColors.of(club.getRealTeamSourceId())`, and tints its accent views programmatically — there is no global theme swap or layout duplication. Coverage:

| Screen | Themed elements |
|---|---|
| `DraftCanvasFragment` | "Pokreni sezonu" button, club name, synergy "Detalji" link |
| `SeasonHubFragment` | Simulate button, club name, matchday label, the user's standings row (club-coloured text + `rowTint()` background) |
| `MatchdayFragment` | Simulate button, simulation progress bar, the user's club name in fixture cards (`MatchdayAdapter.setAccentColor`) |
| `SeasonSummaryFragment` | "Nova sezona" button, the user-result card stroke + label + position badge |

Non-user standings rows keep their existing badge-palette tint. Note: La Liga's FC Barcelona and the Premier League's Tottenham both use `team_id 73` in the seed data (a pre-existing `real_teams` key collision); id 73 keeps the Tottenham theme.

### Bench & Squad Editing

A squad is **11 starters + up to 7 substitutes** (`DraftViewModel.MAX_BENCH`). The bench needed no schema change — bench players are simply `user_squad` rows with `isStartingXI = false` and a null `pitchPosition`. `DraftViewModel.init()` restores both the XI (by pitch slot) and the bench from `user_squad` on load.

The pitch canvas (`DraftCanvasFragment`) runs in **two modes**, both driven by the same `DraftViewModel`:

- **Draft mode** — the initial squad build: buy starters into slots and substitutes onto the bench within budget.
- **Edit mode** (`editMode` nav arg) — reached any time from the Season Hub's **"Uredi sastav i formaciju"** button. Rearrange owned players between the XI and the bench and change formation; no buying or selling. The bottom button reads "Spremi sastav" and simply pops back to the hub.

Because a matchday is simulated in a single shot (no live "match in progress" state), squad changes are made **between matchdays** — the editor is always one tap away from the Season Hub.

Key `DraftViewModel` operations (each persists to `user_squad` immediately and runs on a background thread):

| Operation | Effect |
|---|---|
| `buyForBench()` / `sellFromBench()` | Add/remove a substitute (draft mode only) |
| `moveToBench(slotKey)` | Demote a starter to the bench |
| `assignFromBench(player, slotKey)` | Promote a substitute; any current occupant drops to the bench |
| `changeFormation(name)` | Best-fit current starters into the new formation's slots by position group; overflow goes to the bench; updates `UserClub.formation` |

### Dynamic Player Growth

Player ratings are **not static** — they rise and fall with in-season form, league-wide. Each `RealPlayer` carries a `formDelta` (DB column, migration 6→7); the **effective rating** used by simulation, synergy and every UI display is `getEffectiveOverall() = overall + formDelta`. `formDelta` is reset to 0 for all players when a new season's fixtures are generated.

After every matchday, `SeasonRepository.applyPlayerGrowth()` adjusts `formDelta` for the players who featured:

- **+2** per goal scored
- **+1** per assist
- **+1** for a win, **−1** for a loss (0 for a draw) — applied to the whole lineup

The lineup is the user's real starting XI for their club, and `TraitEngine.bestXi()` as a proxy for AI clubs. Goal/assist credit matches a simulated scorer name to a player in that lineup (exact, then surname); the win/loss component is always exact. Cumulative `formDelta` is clamped to **−10…+12**.

To support assists, `MatchSimulator.Scorer` gained an optional `assist` field — the Gemini prompt asks for the assisting team-mate, and `LocalSimulator` picks a plausible assister in the fallback path.

---

## 11. Navigation Graph

All navigation is in a single `nav_graph.xml`. Start destination: `nav_home`.

```
nav_home

nav_leagues (bottom-nav tab — LeaguesFragment, direct list of seeded leagues)
  └──► nav_teams_list ──► nav_team_detail ──► nav_players ──► nav_player_detail

nav_teams  (TeamsFragment destination — kept in the graph; no longer a bottom-nav tab)

nav_ai_lab (bottom-nav tab)
  ├──► playerSimulation
  └──► teamSimulation

nav_season (bottom-nav tab — LeaguePickerFragment)
  └──► nav_club_mode
          ├──► nav_club_setup ──► nav_formation_picker ──┐
          │                                              ├──► nav_draft_canvas
          └──► nav_existing_club_picker ──────────────────┘
                                              └──► nav_season_hub  ◄── season main loop
                                                        ├──► nav_matchday
                                                        └──► nav_season_summary
```

**Bottom navigation** has **four tabs**: Home, Lige, AI Lab, Sezona. (The former "Timovi" tab was removed; team browsing is still reached via Lige → team list.) The bar is shown only on the top-level tab destinations plus `nav_season_hub`; all sub-screens hide it (`MainActivity` destination-change listener).

---

## 12. Key Patterns & Conventions

### MVVM with LiveData

Every interactive screen has a matching ViewModel. Fragments never call Room or Retrofit directly. All DB writes happen on background threads (via `new Thread(() -> { ... }).start()`). LiveData results are posted with `postValue()` from background threads.

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

Fragments receive data via `getArguments().getInt("key")` / `getString("key")`. Args are declared in `nav_graph.xml` with `app:argType` (raw Bundle, no safe-args plugin).

### Formations

`season/FormationCatalog.java` is the single source of truth for formations. Each formation maps to an ordered list of `FormationSlot`s (key, label, position group, `xPct`, `yPct`). `matchesPosGroup()` decides which seed positions are eligible for a `GK` / `DF` / `MF` / `FW` slot.

### UI theme

Background: `#080A12` (near-black dark blue)
Cards: `#1A1D24` (dark card surface)
Accent: `#2962FF` (Material Blue 700)
Text: `#FFFFFF` primary, `#88FFFFFF` secondary (50% alpha)
User team highlight: translucent accent blue
Champion highlight: gold

Dialogs use a custom dark Material style (`FootMashDialog` in `themes.xml`) — a rounded `#1A1D24` surface with light text and accent-blue buttons — applied via `MaterialAlertDialogBuilder` so they match the app instead of the default light dialog.

The `#2962FF` accent is the **default**. In Season Mode it is replaced at runtime by the inherited club's own colours — see [Per-Club Theming](#10-season-mode--full-game-flow) in section 10.

---

## 13. File Reference

### Season Mode files — quick lookup

| File | Purpose |
|---|---|
| `ui/season/LeaguePickerFragment.java` | Pick one of 5 seeded leagues |
| `ui/season/ClubModeFragment.java` | Choose new-club vs existing-club path |
| `ui/season/ClubSetupFragment.java` | Name a new club, allocate €100M budget |
| `ui/season/FormationPickerFragment.java` | Pick the formation for a new club |
| `ui/season/ExistingClubPickerFragment.java` | Inherit a real club (€50M, pre-seeded XI) |
| `repository/DraftRepository.java` | `seedExistingClub()` — best-fit a real roster into slots |
| `ui/season/DraftViewModel.java` | Draft/edit state: XI assignments, bench, budget, market, formation, synergy |
| `ui/season/DraftCanvasFragment.java` | Pitch canvas + bench + formation chips; draft and mid-season edit modes |
| `ui/season/PlayerMarketBottomSheet.java` | Slot/bench market with trait chips, name search and position filter |
| `season/FormationCatalog.java` | The 4 formations + their pitch slots + position matching |
| `season/TraitEngine.java` | Trait derivation, squad synergy, best-XI / effective rating |
| `model/Trait.java` | Enum of 19 playstyle traits (4–5 per position group) |
| `model/SynergyResult.java` | Synergy rating + delta + active-combo breakdown |
| `ui/util/ClubColors.java` | Per-club colour themes (100 clubs) — re-skins Season Mode |
| `model/FormationSlot.java` | Pitch position descriptor (key, label, posGroup, xPct, yPct) |
| `model/Fixture.java` | Room entity: scheduled match |
| `model/MatchResult.java` | Room entity: simulated match score |
| `model/GoalScorer.java` | Room entity: individual goal event |
| `model/SeasonStanding.java` | Room entity: live league table row |
| `db/FixtureDao.java` | All fixture/result/scorer queries |
| `db/StandingDao.java` | Standing queries + top scorer aggregate |
| `repository/SeasonRepository.java` | `startSeasonIfNeeded()`, `startNextSeason()`, `simulateMatchday()`, `recalculateStandings()`, `applyPlayerGrowth()` |
| `ui/season/SeasonHubViewModel.java` | Standings, next fixture/matchday LiveData, whole-season simulation |
| `ui/season/SeasonHubFragment.java` | Season dashboard; per-matchday and whole-season simulation, routes to Matchday/Summary |
| `ai/MatchSimulator.java` | Gemini prompt builder + JSON parser |
| `ai/LocalSimulator.java` | Probability fallback simulator |
| `ui/season/MatchdayViewModel.java` | SimState machine, `simulate()`, display building |
| `ui/season/MatchdayAdapter.java` | Fixture cards pre/post simulation |
| `ui/season/MatchdayFragment.java` | Simulate button, progress bar, results list |
| `ui/season/SeasonSummaryViewModel.java` | Loads champion, golden boot, user final stats |
| `ui/season/SeasonSummaryFragment.java` | End-of-season display; continue to next season or start a new club |

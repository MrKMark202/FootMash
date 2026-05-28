package hr.fipu.footmash.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hr.fipu.footmash.model.Fixture;

/**
 * Unit tests for the Gemini prompt builder and response parser. Pure logic —
 * no network calls and no Android dependencies.
 */
public class MatchSimulatorTest {

    private static Fixture fixture(String home, String away) {
        Fixture f = new Fixture();
        f.setHomeTeamName(home);
        f.setAwayTeamName(away);
        return f;
    }

    // ─── parseResponse — happy path ──────────────────────────────────────────

    @Test
    public void parsesCleanJsonArray() {
        String raw = "[{\"home_goals\":2,\"away_goals\":1,\"scorers\":["
                   + "{\"name\":\"Smith\",\"team\":\"home\",\"minute\":12},"
                   + "{\"name\":\"Smith\",\"team\":\"home\",\"minute\":67},"
                   + "{\"name\":\"Jones\",\"team\":\"away\",\"minute\":45}]}]";
        List<MatchSimulator.ParsedMatch> result = MatchSimulator.parseResponse(raw, 1);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).homeGoals);
        assertEquals(1, result.get(0).awayGoals);
        assertEquals(3, result.get(0).scorers.size());
    }

    @Test
    public void parsesJsonEmbeddedInProse() {
        // Real Gemini responses often wrap the array in markdown / commentary.
        String raw = "Sure! Here are the results:\n```json\n"
                   + "[{\"home_goals\":0,\"away_goals\":0,\"scorers\":[]}]\n"
                   + "```\nLet me know if you need anything else.";
        List<MatchSimulator.ParsedMatch> result = MatchSimulator.parseResponse(raw, 1);
        assertNotNull(result);
        assertEquals(0, result.get(0).homeGoals);
        assertTrue(result.get(0).scorers.isEmpty());
    }

    // ─── parseResponse — clamping ────────────────────────────────────────────

    @Test
    public void clampsAbsurdScores() {
        // Gemini occasionally hallucinates double-digit scores. The parser caps at 7.
        String raw = "[{\"home_goals\":15,\"away_goals\":-3,\"scorers\":[]}]";
        List<MatchSimulator.ParsedMatch> result = MatchSimulator.parseResponse(raw, 1);
        assertNotNull(result);
        assertEquals(7, result.get(0).homeGoals);
        assertEquals(0, result.get(0).awayGoals);
    }

    @Test
    public void defaultsMissingScorersListToEmpty() {
        String raw = "[{\"home_goals\":1,\"away_goals\":0}]";
        List<MatchSimulator.ParsedMatch> result = MatchSimulator.parseResponse(raw, 1);
        assertNotNull(result);
        assertNotNull("scorers must never be null after parsing", result.get(0).scorers);
    }

    // ─── parseResponse — failure modes (trigger fallback) ────────────────────

    @Test
    public void rejectsWrongElementCount() {
        // Caller expected 2 matches, model returned 1 → null triggers retry/fallback.
        String raw = "[{\"home_goals\":1,\"away_goals\":0,\"scorers\":[]}]";
        assertNull(MatchSimulator.parseResponse(raw, 2));
    }

    @Test
    public void rejectsNullOrEmptyInput() {
        assertNull(MatchSimulator.parseResponse(null, 1));
        assertNull(MatchSimulator.parseResponse("", 1));
    }

    @Test
    public void rejectsMalformedJson() {
        assertNull(MatchSimulator.parseResponse("not json at all", 1));
        assertNull(MatchSimulator.parseResponse("[{broken", 1));
    }

    // ─── buildPrompt — content sanity ────────────────────────────────────────

    @Test
    public void promptMentionsAllFixturesAndExactCount() {
        List<Fixture> fixtures = Arrays.asList(
            fixture("Arsenal", "Chelsea"),
            fixture("Liverpool", "Everton")
        );
        Map<String, Integer> ratings = new HashMap<>();
        ratings.put("Arsenal", 85);
        ratings.put("Chelsea", 83);
        String prompt = MatchSimulator.buildPrompt(fixtures, null, ratings);

        assertTrue("prompt declares count",   prompt.contains("Simulate 2"));
        assertTrue("prompt names home team",  prompt.contains("Arsenal"));
        assertTrue("prompt names away team",  prompt.contains("Chelsea"));
        assertTrue("prompt embeds ratings",   prompt.contains("OVR 85"));
        assertTrue("prompt names second match", prompt.contains("Liverpool"));
    }

    @Test
    public void everyPromptIncludesRealismPreamble() {
        List<Fixture> fixtures = Collections.singletonList(fixture("A", "B"));
        String full   = MatchSimulator.buildPrompt(fixtures, null, Collections.emptyMap());
        String simple = MatchSimulator.buildSimplePrompt(fixtures, Collections.emptyMap());
        assertTrue("full prompt must lead with realism preamble",
            full.startsWith(MatchSimulator.REALISM_PREAMBLE));
        assertTrue("simple fallback prompt must also lead with realism preamble",
            simple.startsWith(MatchSimulator.REALISM_PREAMBLE));
    }

    @Test
    public void preambleMentionsTopScorersAndTeamTiering() {
        // Sanity tests so the constant doesn't silently lose its key clauses
        // if someone reformats it later.
        String p = MatchSimulator.REALISM_PREAMBLE;
        assertTrue("preamble names elite contender clubs", p.contains("Manchester City"));
        assertTrue("preamble bans wing-backs from leading the scoring chart",
            p.contains("wing-back") || p.contains("Frimpong"));
        assertTrue("preamble bans severe shocks", p.contains("severe shocks")
            || p.contains("Leicester"));
    }

    @Test
    public void simplePromptOmitsUserTeamDetails() {
        List<Fixture> fixtures = Collections.singletonList(fixture("A", "B"));
        String prompt = MatchSimulator.buildSimplePrompt(fixtures, Collections.emptyMap());
        assertTrue(prompt.contains("Simulate 1"));
        assertTrue(prompt.contains("A vs B"));
        // The simple prompt is the retry fallback — it must not reference the
        // detailed Starting-XI block that the full prompt emits.
        assertTrue("simple prompt is shorter than full prompt",
            prompt.length() < MatchSimulator.buildPrompt(fixtures, null, Collections.emptyMap()).length());
    }
}

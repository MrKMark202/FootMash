package hr.fipu.footmash.api;

import hr.fipu.footmash.model.ApiResponse;
import hr.fipu.footmash.model.CountryResponse;
import hr.fipu.footmash.model.LeagueResponse;
import hr.fipu.footmash.model.PlayerResponse;
import hr.fipu.footmash.model.StandingResponse;
import hr.fipu.footmash.model.TeamResponse;
import hr.fipu.footmash.model.TeamStatisticsResponse;
import hr.fipu.footmash.model.TopScorerResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit interface za sve API-Football v3 endpoint-e.
 */
public interface FootballApi {

    // ===== COUNTRIES =====
    @GET("countries")
    Call<ApiResponse<CountryResponse>> getCountries();

    @GET("countries")
    Call<ApiResponse<CountryResponse>> searchCountries(@Query("search") String search);

    // ===== LEAGUES =====
    @GET("leagues")
    Call<ApiResponse<LeagueResponse>> getLeagues();

    @GET("leagues")
    Call<ApiResponse<LeagueResponse>> getLeaguesByCountry(@Query("country") String country);

    @GET("leagues")
    Call<ApiResponse<LeagueResponse>> getLeagueById(@Query("id") int id);

    @GET("leagues")
    Call<ApiResponse<LeagueResponse>> getLeaguesByCountryAndSeason(
            @Query("country") String country,
            @Query("season") int season
    );

    // ===== TEAMS =====
    @GET("teams")
    Call<ApiResponse<TeamResponse>> getTeamsByLeague(
            @Query("league") int leagueId,
            @Query("season") int season
    );

    @GET("teams")
    Call<ApiResponse<TeamResponse>> getTeamById(@Query("id") int id);

    @GET("teams")
    Call<ApiResponse<TeamResponse>> searchTeams(@Query("search") String search);

    // ===== TEAM STATISTICS =====
    @GET("teams/statistics")
    Call<TeamStatisticsResponse> getTeamStatistics(
            @Query("league") int leagueId,
            @Query("team") int teamId,
            @Query("season") int season
    );

    // ===== PLAYERS =====
    @GET("players")
    Call<ApiResponse<PlayerResponse>> getPlayersByTeam(
            @Query("team") int teamId,
            @Query("season") int season
    );

    @GET("players")
    Call<ApiResponse<PlayerResponse>> getPlayersByTeamAndPage(
            @Query("team") int teamId,
            @Query("season") int season,
            @Query("page") int page
    );

    @GET("players")
    Call<ApiResponse<PlayerResponse>> searchPlayers(
            @Query("search") String search,
            @Query("league") int leagueId,
            @Query("season") int season
    );

    // ===== STANDINGS =====
    @GET("standings")
    Call<ApiResponse<StandingResponse>> getStandings(
            @Query("league") int leagueId,
            @Query("season") int season
    );

    // ===== TOP SCORERS =====
    @GET("players/topscorers")
    Call<ApiResponse<TopScorerResponse>> getTopScorers(
            @Query("league") int leagueId,
            @Query("season") int season
    );
}

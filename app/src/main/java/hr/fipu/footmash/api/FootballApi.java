package hr.fipu.footmash.api;

import hr.fipu.footmash.model.AllSportsResponse;
import hr.fipu.footmash.model.AllSportsStandingsResponse;
import hr.fipu.footmash.model.CountryResponse;
import hr.fipu.footmash.model.LeagueResponse;
import hr.fipu.footmash.model.PlayerResponse;
import hr.fipu.footmash.model.StandingResponse;
import hr.fipu.footmash.model.TeamResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit interface za AllSportsAPI football endpoint-e.
 */
public interface FootballApi {

    @GET("./")
    Call<AllSportsResponse<CountryResponse>> getCountries(
            @Query("met") String method // Use "Countries"
    );

    @GET("./")
    Call<AllSportsResponse<LeagueResponse>> getLeagues(
            @Query("met") String method // Use "Leagues"
    );

    @GET("./")
    Call<AllSportsResponse<TeamResponse>> getTeamsByLeague(
            @Query("met") String met,
            @Query("league_id") int leagueId
    );

    @GET("./")
    Call<AllSportsResponse<TeamResponse>> getTeamDetail(
            @Query("met") String met,
            @Query("team_id") int teamId
    );

    @GET("./")
    Call<AllSportsResponse<PlayerResponse>> getPlayersByTeam(
            @Query("met") String method, // Use "Players"
            @Query("team_id") int teamId
    );

    @GET("./")
    Call<AllSportsStandingsResponse> getStandings(
            @Query("met") String method, // Use "Standings"
            @Query("league_id") int leagueId
    );

    @GET("./")
    Call<AllSportsResponse<LeagueResponse>> searchLeaguesByName(
            @Query("met") String method, // Use "Leagues"
            @Query("league_id") int leagueId 
    );
}

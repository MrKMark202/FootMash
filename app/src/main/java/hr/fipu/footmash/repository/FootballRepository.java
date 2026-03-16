package hr.fipu.footmash.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.api.ApiClient;
import hr.fipu.footmash.api.FootballApi;
import hr.fipu.footmash.model.ApiResponse;
import hr.fipu.footmash.model.CountryResponse;
import hr.fipu.footmash.model.LeagueResponse;
import hr.fipu.footmash.model.PlayerResponse;
import hr.fipu.footmash.model.StandingResponse;
import hr.fipu.footmash.model.TeamResponse;
import hr.fipu.footmash.model.TeamStatisticsResponse;
import hr.fipu.footmash.model.TopScorerResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository koji koristi FootballApi za dohvaćanje podataka
 * i izlaže rezultate kroz LiveData.
 */
public class FootballRepository {

    private static final String TAG = "FootballRepository";
    private final FootballApi api;

    public FootballRepository() {
        this.api = ApiClient.getApi();
    }

    // ===== COUNTRIES =====
    public LiveData<List<CountryResponse>> getCountries() {
        MutableLiveData<List<CountryResponse>> data = new MutableLiveData<>();
        api.getCountries().enqueue(new Callback<ApiResponse<CountryResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<CountryResponse>> call, Response<ApiResponse<CountryResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(response.body().getResponse());
                } else {
                    Log.e(TAG, "getCountries error: " + response.code());
                    data.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<CountryResponse>> call, Throwable t) {
                Log.e(TAG, "getCountries failed", t);
                data.setValue(new ArrayList<>());
            }
        });
        return data;
    }

    // ===== LEAGUES =====
    public LiveData<List<LeagueResponse>> getLeaguesByCountry(String country) {
        MutableLiveData<List<LeagueResponse>> data = new MutableLiveData<>();
        api.getLeaguesByCountry(country).enqueue(new Callback<ApiResponse<LeagueResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LeagueResponse>> call, Response<ApiResponse<LeagueResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(response.body().getResponse());
                } else {
                    Log.e(TAG, "getLeagues error: " + response.code());
                    data.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LeagueResponse>> call, Throwable t) {
                Log.e(TAG, "getLeagues failed", t);
                data.setValue(new ArrayList<>());
            }
        });
        return data;
    }

    public LiveData<List<LeagueResponse>> getAllLeagues() {
        MutableLiveData<List<LeagueResponse>> data = new MutableLiveData<>();
        api.getLeagues().enqueue(new Callback<ApiResponse<LeagueResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LeagueResponse>> call, Response<ApiResponse<LeagueResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(response.body().getResponse());
                } else {
                    data.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LeagueResponse>> call, Throwable t) {
                Log.e(TAG, "getAllLeagues failed", t);
                data.setValue(new ArrayList<>());
            }
        });
        return data;
    }

    // ===== TEAMS =====
    public LiveData<List<TeamResponse>> getTeamsByLeague(int leagueId, int season) {
        MutableLiveData<List<TeamResponse>> data = new MutableLiveData<>();
        api.getTeamsByLeague(leagueId, season).enqueue(new Callback<ApiResponse<TeamResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TeamResponse>> call, Response<ApiResponse<TeamResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(response.body().getResponse());
                } else {
                    Log.e(TAG, "getTeams error: " + response.code());
                    data.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TeamResponse>> call, Throwable t) {
                Log.e(TAG, "getTeams failed", t);
                data.setValue(new ArrayList<>());
            }
        });
        return data;
    }

    // ===== TEAM STATISTICS =====
    public LiveData<TeamStatisticsResponse.TeamStats> getTeamStatistics(int leagueId, int teamId, int season) {
        MutableLiveData<TeamStatisticsResponse.TeamStats> data = new MutableLiveData<>();
        api.getTeamStatistics(leagueId, teamId, season).enqueue(new Callback<TeamStatisticsResponse>() {
            @Override
            public void onResponse(Call<TeamStatisticsResponse> call, Response<TeamStatisticsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(response.body().getResponse());
                } else {
                    Log.e(TAG, "getTeamStats error: " + response.code());
                    data.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<TeamStatisticsResponse> call, Throwable t) {
                Log.e(TAG, "getTeamStats failed", t);
                data.setValue(null);
            }
        });
        return data;
    }

    // ===== PLAYERS =====
    public LiveData<List<PlayerResponse>> getPlayersByTeam(int teamId, int season) {
        MutableLiveData<List<PlayerResponse>> data = new MutableLiveData<>();
        api.getPlayersByTeam(teamId, season).enqueue(new Callback<ApiResponse<PlayerResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<PlayerResponse>> call, Response<ApiResponse<PlayerResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(response.body().getResponse());
                } else {
                    Log.e(TAG, "getPlayers error: " + response.code());
                    data.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PlayerResponse>> call, Throwable t) {
                Log.e(TAG, "getPlayers failed", t);
                data.setValue(new ArrayList<>());
            }
        });
        return data;
    }

    // ===== STANDINGS =====
    public LiveData<List<StandingResponse>> getStandings(int leagueId, int season) {
        MutableLiveData<List<StandingResponse>> data = new MutableLiveData<>();
        api.getStandings(leagueId, season).enqueue(new Callback<ApiResponse<StandingResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<StandingResponse>> call, Response<ApiResponse<StandingResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(response.body().getResponse());
                } else {
                    Log.e(TAG, "getStandings error: " + response.code());
                    data.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<StandingResponse>> call, Throwable t) {
                Log.e(TAG, "getStandings failed", t);
                data.setValue(new ArrayList<>());
            }
        });
        return data;
    }

    // ===== TOP SCORERS =====
    public LiveData<List<TopScorerResponse>> getTopScorers(int leagueId, int season) {
        MutableLiveData<List<TopScorerResponse>> data = new MutableLiveData<>();
        api.getTopScorers(leagueId, season).enqueue(new Callback<ApiResponse<TopScorerResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TopScorerResponse>> call, Response<ApiResponse<TopScorerResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(response.body().getResponse());
                } else {
                    Log.e(TAG, "getTopScorers error: " + response.code());
                    data.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TopScorerResponse>> call, Throwable t) {
                Log.e(TAG, "getTopScorers failed", t);
                data.setValue(new ArrayList<>());
            }
        });
        return data;
    }
}

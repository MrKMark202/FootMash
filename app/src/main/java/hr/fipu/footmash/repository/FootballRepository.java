package hr.fipu.footmash.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import hr.fipu.footmash.api.ApiClient;
import hr.fipu.footmash.api.FootballApi;
import hr.fipu.footmash.model.AllSportsResponse;
import hr.fipu.footmash.model.AllSportsStandingsResponse;
import hr.fipu.footmash.model.CountryResponse;
import hr.fipu.footmash.model.LeagueResponse;
import hr.fipu.footmash.model.PlayerResponse;
import hr.fipu.footmash.model.StandingResponse;
import hr.fipu.footmash.model.TeamResponse;
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
        api.getCountries("Countries").enqueue(new Callback<AllSportsResponse<CountryResponse>>() {
            @Override
            public void onResponse(Call<AllSportsResponse<CountryResponse>> call, Response<AllSportsResponse<CountryResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(response.body().getResult());
                } else {
                    data.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<AllSportsResponse<CountryResponse>> call, Throwable t) {
                data.setValue(new ArrayList<>());
            }
        });
        return data;
    }

    // ===== LEAGUES =====
    public LiveData<List<LeagueResponse>> getAllLeagues() {
        MutableLiveData<List<LeagueResponse>> data = new MutableLiveData<>();
        api.getLeagues("Leagues").enqueue(new Callback<AllSportsResponse<LeagueResponse>>() {
            @Override
            public void onResponse(Call<AllSportsResponse<LeagueResponse>> call, Response<AllSportsResponse<LeagueResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(response.body().getResult());
                } else {
                    Log.e(TAG, "getAllLeagues error: " + response.code());
                    data.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<AllSportsResponse<LeagueResponse>> call, Throwable t) {
                Log.e(TAG, "getAllLeagues failed", t);
                data.setValue(new ArrayList<>());
            }
        });
        return data;
    }

    // ===== TEAMS =====
    public LiveData<List<TeamResponse>> getTeamsByLeague(int leagueId, int season) {
        MutableLiveData<List<TeamResponse>> data = new MutableLiveData<>();
        api.getTeamsByLeague("Teams", leagueId).enqueue(new Callback<AllSportsResponse<TeamResponse>>() {
            @Override
            public void onResponse(Call<AllSportsResponse<TeamResponse>> call, Response<AllSportsResponse<TeamResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(response.body().getResult());
                } else {
                    Log.e(TAG, "getTeams error: " + response.code());
                    data.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<AllSportsResponse<TeamResponse>> call, Throwable t) {
                Log.e(TAG, "getTeams failed", t);
                data.setValue(new ArrayList<>());
            }
        });
        return data;
    }

    public LiveData<List<TeamResponse>> getTeamDetail(int teamId) {
        MutableLiveData<List<TeamResponse>> data = new MutableLiveData<>();
        api.getTeamDetail("Teams", teamId).enqueue(new Callback<AllSportsResponse<TeamResponse>>() {
            @Override
            public void onResponse(Call<AllSportsResponse<TeamResponse>> call, Response<AllSportsResponse<TeamResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(response.body().getResult());
                } else {
                    data.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<AllSportsResponse<TeamResponse>> call, Throwable t) {
                data.setValue(new ArrayList<>());
            }
        });
        return data;
    }

    // ===== PLAYERS =====
    public LiveData<List<PlayerResponse>> getPlayersByTeam(int teamId, int season) {
        MutableLiveData<List<PlayerResponse>> data = new MutableLiveData<>();
        api.getPlayersByTeam("Players", teamId).enqueue(new Callback<AllSportsResponse<PlayerResponse>>() {
            @Override
            public void onResponse(Call<AllSportsResponse<PlayerResponse>> call, Response<AllSportsResponse<PlayerResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(response.body().getResult());
                } else {
                    Log.e(TAG, "getPlayers error: " + response.code());
                    data.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<AllSportsResponse<PlayerResponse>> call, Throwable t) {
                Log.e(TAG, "getPlayers failed", t);
                data.setValue(new ArrayList<>());
            }
        });
        return data;
    }

    // ===== STANDINGS =====
    public LiveData<List<StandingResponse>> getStandings(int leagueId, int season) {
        MutableLiveData<List<StandingResponse>> data = new MutableLiveData<>();
        api.getStandings("Standings", leagueId).enqueue(new Callback<AllSportsStandingsResponse>() {
            @Override
            public void onResponse(Call<AllSportsStandingsResponse> call, Response<AllSportsStandingsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    data.setValue(response.body().getResult().getTotal());
                } else {
                    Log.e(TAG, "getStandings error or null result");
                    data.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<AllSportsStandingsResponse> call, Throwable t) {
                Log.e(TAG, "getStandings failed", t);
                data.setValue(new ArrayList<>());
            }
        });
        return data;
    }
}

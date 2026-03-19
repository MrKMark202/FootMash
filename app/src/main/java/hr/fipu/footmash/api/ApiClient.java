package hr.fipu.footmash.api;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton Retrofit klijent za API-Football v3.
 * Base URL: https://v3.football.api-sports.io/
 * Auth header: x-apisports-key
 */
public class ApiClient {

    private static final String BASE_URL = "https://apiv2.allsportsapi.com/football/";
    private static Retrofit retrofit = null;
    private static String apiKey = hr.fipu.footmash.BuildConfig.FOOTBALL_API_KEY.replace("\"", "");

    public static void setApiKey(String key) {
        apiKey = key;
        retrofit = null; // Force rebuild
    }

    public static String getApiKey() {
        return apiKey;
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        okhttp3.HttpUrl url = chain.request().url().newBuilder()
                                .addQueryParameter("APIkey", apiKey)
                                .build();
                        okhttp3.Request request = chain.request().newBuilder()
                                .url(url)
                                .build();
                        return chain.proceed(request);
                    })
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static FootballApi getApi() {
        return getClient().create(FootballApi.class);
    }
}

package hr.fipu.footmash.ai;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiApi {
    
    // Model koristimo gemini-1.5-flash ili gemini-1.5-pro
    @Headers("Content-Type: application/json")
    @POST("models/gemini-1.5-flash:generateContent")
    Call<GeminiResponse> generateContent(@Query("key") String apiKey, @Body GeminiRequest request);
}

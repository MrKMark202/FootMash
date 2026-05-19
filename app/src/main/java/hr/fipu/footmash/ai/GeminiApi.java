package hr.fipu.footmash.ai;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiApi {
    
    // Koristimo aktualni Gemini 2.0 Flash model (gemini-1.5-flash je povučen).
    @Headers("Content-Type: application/json")
    @POST("models/gemini-2.0-flash:generateContent")
    Call<GeminiResponse> generateContent(@Query("key") String apiKey, @Body GeminiRequest request);
}

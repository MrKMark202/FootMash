package hr.fipu.footmash.ai;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GeminiRepository {
    private final GeminiApi api;

    public GeminiRepository() {
        api = GeminiClient.getClient().create(GeminiApi.class);
    }

    public LiveData<String> generateSimulation(String prompt, String apiKey) {
        MutableLiveData<String> liveData = new MutableLiveData<>();

        GeminiRequest.Part part = new GeminiRequest.Part(prompt);
        List<GeminiRequest.Part> parts = new ArrayList<>();
        parts.add(part);

        GeminiRequest.Content content = new GeminiRequest.Content("user", parts);
        List<GeminiRequest.Content> contents = new ArrayList<>();
        contents.add(content);

        GeminiRequest request = new GeminiRequest(contents);

        api.generateContent(apiKey, request).enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    liveData.setValue(response.body().getResponseText());
                } else {
                    liveData.setValue("Greška: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                liveData.setValue("Greška mreže: " + t.getMessage());
            }
        });

        return liveData;
    }
}

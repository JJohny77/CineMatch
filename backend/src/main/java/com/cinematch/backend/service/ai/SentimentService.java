package com.cinematch.backend.service.ai;

import com.cinematch.backend.dto.SentimentResponse;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class SentimentService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${HUGGINGFACE_API_KEY}")
    private String apiKey;

    private static final String MODEL_URL =
            "https://router.huggingface.co/hf-inference/models/distilbert/distilbert-base-uncased-finetuned-sst-2-english";

    public SentimentResponse analyze(String text) {

        JSONObject payload = new JSONObject();
        payload.put("inputs", text);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 🔥 ΠΡΟΣΟΧΗ: ΤΟ ΣΩΣΤΟ HEADER (όχι Authorization)
        headers.set("HF-Api-Key", apiKey);

        HttpEntity<String> request =
                new HttpEntity<>(payload.toString(), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                MODEL_URL,
                HttpMethod.POST,
                request,
                String.class
        );

        // Αν το μοντέλο φορτώνει
        if (response.getBody().contains("loading")) {
            return new SentimentResponse("loading", 0.0);
        }

        // Σωστό parsing της JSON από το HuggingFace Router API
        JSONArray outer = new JSONArray(response.getBody());
        JSONArray predictions = outer.getJSONArray(0);
        JSONObject result = predictions.getJSONObject(0);

        String label = result.getString("label");
        double score = result.getDouble("score");

        return new SentimentResponse(label, score);
    }
}

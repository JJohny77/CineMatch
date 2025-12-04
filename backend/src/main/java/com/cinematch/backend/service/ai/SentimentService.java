package com.cinematch.backend.service.ai;

import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.cinematch.backend.dto.SentimentResponse;

@Service
@RequiredArgsConstructor
public class SentimentService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${huggingface.api.key:}")
    private String apiKey;

    private static final String MODEL_URL =
            "https://router.huggingface.co/hf-inference/models/distilbert/distilbert-base-uncased-finetuned-sst-2-english";

    public SentimentResponse analyze(String text) {

        // 🔐 Safety check για να μη στέλνουμε άδειο token
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "HuggingFace API key is missing. " +
                            "Set HUGGINGFACE_API_KEY in your .env or environment."
            );
        }

        // ---- 1. Payload ----
        JSONObject payload = new JSONObject();
        payload.put("inputs", text);

        // ---- 2. Headers ----
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, "application/json");
        headers.add("X-Response-Format", "json");

        // Authorization: Bearer hf_xxx
        headers.setBearerAuth(apiKey);

        HttpEntity<String> request = new HttpEntity<>(payload.toString(), headers);

        // ---- 3. Execute request ----
        ResponseEntity<String> response = restTemplate.exchange(
                MODEL_URL,
                HttpMethod.POST,
                request,
                String.class
        );

        String body = response.getBody();

        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Empty response from HuggingFace");
        }

        // HF Router όταν το μοντέλο “ξυπνάει”/φορτώνει
        if (body.contains("\"estimated_time\"") || body.contains("\"error\"")) {
            return new SentimentResponse("loading", 0.0);
        }

        // ---- 4. Parse JSON ----
        JSONArray arr = new JSONArray(body);
        JSONObject bestLabelObj;

        Object first = arr.get(0);

        if (first instanceof JSONObject) {
            bestLabelObj = (JSONObject) first;
        } else if (first instanceof JSONArray) {
            bestLabelObj = ((JSONArray) first).getJSONObject(0);
        } else {
            throw new IllegalStateException("Unknown HF format: " + body);
        }

        String rawLabel = bestLabelObj.getString("label").toLowerCase(); // "positive" ή "negative"
        double score = bestLabelObj.getDouble("score");

        // ---- 5. Neutral Logic ----
        String finalLabel;
        if (score < 0.60) {
            finalLabel = "neutral";
        } else {
            finalLabel = rawLabel; // "positive" ή "negative"
        }

        return new SentimentResponse(finalLabel, score);
    }
}


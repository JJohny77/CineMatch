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

    // 🔥 ΣΩΣΤΟ router endpoint (όπως στο Postman)
    private static final String MODEL_URL =
            "https://router.huggingface.co/hf-inference/models/distilbert/distilbert-base-uncased-finetuned-sst-2-english";

    public SentimentResponse analyze(String text) {

        // ---- 1. Payload ----
        JSONObject payload = new JSONObject();
        payload.put("inputs", text);

        // ---- 2. Headers ----
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);   // Authorization: Bearer hf_xxx

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

        // ---- 4. Model loading state ----
        if (body.contains("\"estimated_time\"") || body.contains("\"error\"")) {
            return new SentimentResponse("loading", 0.0);
        }

        // ---- 5. Parse ----
        JSONArray arr = new JSONArray(body);

        // Η δική σου response είναι flat array: [ {label, score}, {...} ]
        JSONObject bestLabelObj;

        Object first = arr.get(0);
        if (first instanceof JSONObject) {
            bestLabelObj = (JSONObject) first;
        } else if (first instanceof JSONArray) {
            // nested case: [ [ {label, score}, ... ] ]
            bestLabelObj = ((JSONArray) first).getJSONObject(0);
        } else {
            throw new IllegalStateException("Unknown HF response format: " + body);
        }

        String label = bestLabelObj.getString("label").toLowerCase();
        double score = bestLabelObj.getDouble("score");

        return new SentimentResponse(label, score);
    }
}

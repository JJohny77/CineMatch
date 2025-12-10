package com.cinematch.backend.config.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "recast")
public class RecastConfig {

    /**
     * HuggingFace API key (Bearer token)
     */
    private String apiKey;

    /**
     * HF model id στο HuggingFace Hub.
     * Για image embeddings χρησιμοποιούμε:
     *   radames/blip_image_embeddings
     */
    private String model = "radames/blip_image_embeddings";

    /**
     * Βασικό endpoint για HF Inference API (models path).
     * Π.χ. https://api-inference.huggingface.co/models
     */
    private String endpoint = "https://router.huggingface.co/hf-inference/models";
    /**
     * Timeout σε ms
     */
    private int timeout = 20000;

    // Getters & setters

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}

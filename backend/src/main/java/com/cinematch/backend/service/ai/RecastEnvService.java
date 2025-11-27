package com.cinematch.backend.service.ai;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.stereotype.Service;

@Service
public class RecastEnvService {

    private final String apiKey;
    private final String model;

    public RecastEnvService(Dotenv dotenv) {
        this.apiKey = dotenv.get("RECAST_API_KEY");
        this.model  = dotenv.get("RECAST_MODEL", "FaceRecognitionModel");
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }
}

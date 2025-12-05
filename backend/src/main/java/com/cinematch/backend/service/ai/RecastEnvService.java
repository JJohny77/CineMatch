package com.cinematch.backend.service.ai;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RecastEnvService {

    private static final Logger log = LoggerFactory.getLogger(RecastEnvService.class);

    private final String apiKey;
    private final String model;

    public RecastEnvService(Dotenv dotenv) {
        this.apiKey = dotenv.get("RECAST_API_KEY");
        this.model  = dotenv.get("RECAST_MODEL", "google/vit-base-patch16-224");

        log.info("[RecastEnvService] RECAST_API_KEY length = {}",
                apiKey != null ? apiKey.length() : "null");
        log.info("[RecastEnvService] RECAST_MODEL = {}", this.model);
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }
}

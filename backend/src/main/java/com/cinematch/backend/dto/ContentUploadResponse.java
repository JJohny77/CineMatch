package com.cinematch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContentUploadResponse {

    private String status;
    private String filename;
    private String type;
    private String url;
    private Double duration;  // null for images
}

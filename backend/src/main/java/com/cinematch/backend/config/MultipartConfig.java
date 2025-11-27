package com.cinematch.backend.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

@Configuration
public class MultipartConfig {

    // Όρια μεγέθους για upload
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();

        factory.setMaxFileSize(DataSize.ofMegabytes(200));     // max 200MB
        factory.setMaxRequestSize(DataSize.ofMegabytes(200));  // max 200MB

        return factory.createMultipartConfig();
    }

    // 🔥 Ο ΑΠΟΛΥΤΟΣ multipart resolver που ενεργοποιεί το MultipartFile
    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
}

package com.sallejoven.backend.model.dto;

import com.sallejoven.backend.service.S3V2Service;

import java.util.Map;

public record PresignedPutDTO(String url, String key, Map<String,String> requiredHeaders) {
    public static PresignedPutDTO from(S3V2Service.PresignedPut p) {
        return new PresignedPutDTO(p.url(), p.key(), p.requiredHeaders());
    }
}
package com.sallejoven.backend.model.dto;

public record InitiateEventResp(EventDto event, PresignedPutDTO imageUpload, PresignedPutDTO pdfUpload) {}

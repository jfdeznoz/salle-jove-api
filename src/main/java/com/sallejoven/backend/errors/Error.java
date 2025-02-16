package com.sallejoven.backend.errors;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
public class Error {

    private HttpStatus status;
    private int statusCode;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    private String message;
    private String debugMessage;
    private String path;
    private List<ApiSubError> subErrors;

    public Error(HttpStatus status) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.statusCode = status.value();
    }

    public Error(HttpStatus status, String message, String debugMessage) {
        this(status);
        this.message = message;
        this.debugMessage = debugMessage;
    }

    public Error(HttpStatus status, String message, String debugMessage, String path) {
        this(status, message, debugMessage);
        this.path = path;
    }

    public void addSubError(ApiSubError subError) {
        if (this.subErrors == null) {
            this.subErrors = new ArrayList<>();
        }
        this.subErrors.add(subError);
    }

    @Data
    @EqualsAndHashCode
    @AllArgsConstructor
    public static class ApiValidationError implements ApiSubError {
        private String object;
        private String field;
        private Object rejectedValue;
        private String message;

        public ApiValidationError(String object, String message) {
            this.object = object;
            this.message = message;
        }
    }

    public interface ApiSubError {
    }
}
package com.sallejoven.backend.errors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sallejoven.backend.model.types.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper;

    public CustomAuthenticationEntryPoint(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException ex) throws IOException {

        // Evita el popup de Basic Auth
        response.setHeader("WWW-Authenticate", "");

        // Construye tu modelo Error
        var error = new Error(HttpStatus.UNAUTHORIZED);
        error.setMessage(ErrorCodes.INVALID_CREDENTIALS.getMessage());
        error.setDebugMessage(null); // no revelar detalle
        error.setPath(request.getRequestURI());
        error.addSubError(new Error.ApiValidationError(
                "Auth",
                ErrorCodes.INVALID_CREDENTIALS.getErrorCode()
        ));

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getWriter(), error);
    }
}
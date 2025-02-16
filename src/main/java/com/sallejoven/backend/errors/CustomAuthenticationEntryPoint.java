package com.sallejoven.backend.errors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Autowired
    private HttpMessageConverter<String> messageConverter;

    @Autowired
    private ObjectMapper mapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException {
        var error = new Error(HttpStatus.UNAUTHORIZED);
        error.setMessage("Authentication failed");
        error.setDebugMessage(e.getMessage());
        error.setPath(request.getRequestURI());

        try (ServerHttpResponse outputMessage = new ServletServerHttpResponse(response)) {
            outputMessage.setStatusCode(HttpStatus.UNAUTHORIZED);
            messageConverter.write(mapper.writeValueAsString(error), MediaType.APPLICATION_JSON, outputMessage);
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Unable to process authentication error.\"}");
        }
    }
}
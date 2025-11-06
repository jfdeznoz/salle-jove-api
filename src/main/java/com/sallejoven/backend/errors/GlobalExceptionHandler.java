package com.sallejoven.backend.errors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import java.nio.file.AccessDeniedException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Validación de @Valid en @RequestBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Error handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Error out = new Error(HttpStatus.BAD_REQUEST);
        out.setMessage("Validation failed");
        out.setPath(req.getRequestURI());
        out.setDebugMessage("Invalid request body");

        ex.getBindingResult().getFieldErrors().forEach(fe ->
                out.addSubError(new Error.ApiValidationError(
                        fe.getObjectName(), fe.getField(), fe.getRejectedValue(), fe.getDefaultMessage()))
        );
        log.warn("400 Validation failed on {}: {}", req.getRequestURI(),
                ex.getBindingResult().getFieldErrors().stream()
                        .map(fe -> fe.getField() + "=" + fe.getRejectedValue() + " (" + fe.getDefaultMessage() + ")")
                        .collect(Collectors.joining(", ")));
        return out;
    }

    // Validación en query/path (@Validated en controller)
    @ExceptionHandler({ConstraintViolationException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Error handleConstraintViolation(Exception ex, HttpServletRequest req) {
        Error out = new Error(HttpStatus.BAD_REQUEST);
        out.setMessage("Validation failed");
        out.setPath(req.getRequestURI());
        out.setDebugMessage(ex.getMessage());
        log.warn("400 Constraint violation on {}: {}", req.getRequestURI(), ex.getMessage());
        return out;
    }

    // JSON mal formado, tipos erróneos, etc.
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Error handleBadRequest(Exception ex, HttpServletRequest req) {
        Error out = new Error(HttpStatus.BAD_REQUEST);
        out.setMessage("Bad request");
        out.setPath(req.getRequestURI());
        out.setDebugMessage(safe(ex.getMessage()));
        log.warn("400 Bad request on {}: {}", req.getRequestURI(), ex.getMessage());
        return out;
    }

    // Excepciones de negocio que lances con ResponseStatusException
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Error> handleRse(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        Error out = new Error(status != null ? status : HttpStatus.BAD_REQUEST);
        out.setMessage(ex.getReason() != null ? ex.getReason() : "Business error");
        out.setPath(req.getRequestURI());
        out.setDebugMessage(null);
        return ResponseEntity.status(out.getStatusCode()).body(out);
    }

    // Conflictos de datos (índices únicos, FK, etc.)
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Error handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        Error out = new Error(HttpStatus.CONFLICT);
        out.setMessage("Data conflict");
        out.setPath(req.getRequestURI());
        out.setDebugMessage(safe(mostSpecific(ex)));
        log.error("409 Data conflict on {}: {}", req.getRequestURI(), mostSpecific(ex));
        return out;
    }

    // Tu excepción de dominio
    @ExceptionHandler(SalleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Error handleSalle(SalleException ex, HttpServletRequest req) {
        Error out = new Error(HttpStatus.BAD_REQUEST);
        out.setMessage(ex.getMessage());
        out.setPath(req.getRequestURI());
        out.setDebugMessage(ex.getAdditionalInfo());
        // opcional: subError con el código
        out.addSubError(new Error.ApiValidationError("SalleException", ex.getErrorCode()));
        log.warn("400 SalleException on {}: code={} msg={}", req.getRequestURI(), ex.getErrorCode(), ex.getMessage());
        return out;
    }


    @ExceptionHandler({ AuthorizationDeniedException.class, AccessDeniedException.class })
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Error handleAccessDenied(RuntimeException ex, HttpServletRequest req) {
        Error out = new Error(HttpStatus.FORBIDDEN);
        out.setMessage("Access denied");
        out.setPath(req.getRequestURI());
        out.setDebugMessage(null); // opcional: oculta el detalle en prod
        log.warn("403 Access denied on {}: {}", req.getRequestURI(), ex.getMessage());
        return out;
    }

    // Fallback (500)
    @ExceptionHandler({ErrorResponseException.class, Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Error handleAny(Exception ex, HttpServletRequest req) {
        Error out = new Error(HttpStatus.INTERNAL_SERVER_ERROR);
        out.setMessage("Internal Server Error");
        out.setPath(req.getRequestURI());
        out.setDebugMessage(safe(ex.getMessage())); // si prefieres, pon null en prod
        log.error("500 Unhandled exception on {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return out;
    }

    private static String mostSpecific(DataIntegrityViolationException ex) {
        return ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
    }

    private static String safe(String msg) {
        if (msg == null) return null;
        return msg.replaceAll("(?i)(password|authorization|token|secret|apikey)=([^&\\s]+)", "$1=***");
    }
}
package com.sallejoven.backend.controller;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.AuthResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sallejoven.backend.model.dto.UserRegistrationDto;
import com.sallejoven.backend.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    @Lazy
    private final AuthService authService;

    @PostMapping("/sign-in")
    public ResponseEntity<?> authenticateUser(Authentication authentication, HttpServletResponse response){
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid credentials");
        }
        return ResponseEntity.ok(authService.getJwtTokensAfterAuthentication(authentication, response));
    }

    @PostMapping("/refresh-token")
    public AuthResponseDto refresh(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "refresh_token", required = false) String refreshCookie,
            @RequestParam(value = "refresh_token", required = false) String refreshParam, // <-- NUEVO
            HttpServletResponse response
    ) throws SalleException {

        // 1) Prioriza Authorization
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return (AuthResponseDto) authService.getAccessTokenUsingRefreshToken(authHeader, response);
        }

        // 2) Fallback cookie
        if (refreshCookie != null && !refreshCookie.isBlank()) {
            String h = "Bearer " + refreshCookie;
            return (AuthResponseDto) authService.getAccessTokenUsingRefreshToken(h, response);
        }

        // 3) Fallback body (x-www-form-urlencoded)
        if (refreshParam != null && !refreshParam.isBlank()) {
            String h = "Bearer " + refreshParam;
            return (AuthResponseDto) authService.getAccessTokenUsingRefreshToken(h, response);
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing refresh token");
    }

    @PostMapping("/sign-up")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationDto userRegistrationDto,
                                            BindingResult bindingResult,HttpServletResponse httpServletResponse){

        log.info("[AuthController:registerUser]Signup Process Started for user:{}",userRegistrationDto.userName());
        if (bindingResult.hasErrors()) {
            List<String> errorMessage = bindingResult.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
            log.error("[AuthController:registerUser]Errors in user:{}",errorMessage);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
        }
        return ResponseEntity.ok(authService.registerUser(userRegistrationDto,httpServletResponse));
    }
}
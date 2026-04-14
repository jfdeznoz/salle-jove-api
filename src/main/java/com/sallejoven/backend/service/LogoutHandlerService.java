package com.sallejoven.backend.service;

import com.sallejoven.backend.config.security.JwtProperties;
import com.sallejoven.backend.repository.RefreshTokenRepository;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.authentication.logout.LogoutHandler;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogoutHandlerService implements LogoutHandler {

    private final RefreshTokenRepository refreshTokenRepo;
    private final JwtProperties jwtProps;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String refreshToken = extractRefreshTokenCookie(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            log.debug("Logout requested without refresh_token cookie");
            clearRefreshCookie(response);
            return;
        }

        refreshTokenRepo.findByToken(refreshToken)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepo.save(token);
                });

        clearRefreshCookie(response);
    }

    private String extractRefreshTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if ("refresh_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        boolean secure = jwtProps.isCookieSecure();
        String sameSite = secure ? "None" : "Lax";

        ResponseCookie clearCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite(sameSite)
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
    }
}

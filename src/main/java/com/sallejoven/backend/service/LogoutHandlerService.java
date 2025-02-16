package com.sallejoven.backend.service;

import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;

import com.sallejoven.backend.model.enums.TokenType;
import com.sallejoven.backend.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogoutHandlerService implements LogoutHandler {

    private final RefreshTokenRepository refreshTokenRepo;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        
        if(!authHeader.startsWith(TokenType.Bearer.name())){
            return;
        }

        final String refreshToken = authHeader.substring(7);
        
        var storedRefreshToken = refreshTokenRepo.findByToken(refreshToken)
                .map(token->{
                    token.setRevoked(true);
                    refreshTokenRepo.save(token);
                    return token;
                })
                .orElse(null);
    }
}

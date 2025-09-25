package com.sallejoven.backend.config.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;    
    
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtTokenGenerator {


    private final JwtEncoder jwtEncoder;

    public String generateAccessToken(Authentication authentication) {

        log.info("[JwtTokenGenerator:generateAccessToken] Token Creation Started for:{}", authentication.getName());

        List<String> granted = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        List<String> roles = granted.stream()
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .distinct()
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("atquil")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .subject(authentication.getName())
                .claim("roles", roles) // 👈 usamos roles (array), no scope
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public String generateRefreshToken(Authentication authentication) {

        log.info("[JwtTokenGenerator:generateRefreshToken] Token Creation Started for:{}", authentication.getName());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("atquil")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(15, ChronoUnit.DAYS))
                .subject(authentication.getName())
                .claim("roles", List.of("REFRESH")) // opcional
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

}
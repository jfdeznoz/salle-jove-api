package com.sallejoven.backend.config.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.sallejoven.backend.repository.UserRepository;
import com.sallejoven.backend.service.AuthorityService;  // <- tu servicio (el que ya tienes)
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtTokenGenerator {

    private final JwtEncoder jwtEncoder;
    private final UserRepository userRepo;
    private final AuthorityService authorityService;

    public String generateAccessToken(Authentication authentication) {
        log.info("[JwtTokenGenerator] Creating access token for {}", authentication.getName());

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)     // "ROLE_ADMIN"
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length())) // "ADMIN"
                .distinct()
                .toList();

        var user = userRepo.findByEmail(authentication.getName()).orElseThrow();
        List<String> authz;
        try {
            authz = authorityService.buildContextAuthorities(user.getId());
        } catch (Exception e) {
            log.error("Error building context authorities for {}: {}", authentication.getName(), e.getMessage());
            authz = List.of(); // fallback seguro
        }

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("atquil")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .subject(authentication.getName())
                .claim("roles", roles)   // globales, si los usas (p.ej. ["ADMIN"])
                .claim("authz", authz)   // contextuales: ["CENTER:5:GROUP_LEADER", "GROUP:12:ANIMATOR", ...]
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public String generateRefreshToken(Authentication authentication) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("atquil")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(15, ChronoUnit.DAYS))
                .subject(authentication.getName())
                .claim("roles", List.of("REFRESH"))
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
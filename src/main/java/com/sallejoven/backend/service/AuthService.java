package com.sallejoven.backend.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.sallejoven.backend.config.security.JwtProperties;
import com.sallejoven.backend.model.enums.ErrorCodes;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.sallejoven.backend.config.security.JwtTokenGenerator;
import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.mapper.UserInfoMapper;
import com.sallejoven.backend.model.dto.AuthResponseDto;
import com.sallejoven.backend.model.dto.UserRegistrationDto;
import com.sallejoven.backend.model.entity.RefreshToken;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.model.enums.TokenType;
import com.sallejoven.backend.repository.RefreshTokenRepository;
import com.sallejoven.backend.repository.UserRepository;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    //test
    private final UserRepository userInfoRepo;
    private final UserInfoMapper userInfoMapper;
    private final JwtTokenGenerator jwtTokenGenerator;
    private final RefreshTokenRepository refreshTokenRepo;
    private final UserRepository userRepository;
    private final AuthorityService authorityService;
    private final JwtProperties jwtProps;


    public String getCurrentUserEmail(){
        var context = SecurityContextHolder.getContext();
        var authentication = context.getAuthentication();
        return authentication.getName();
    }

    public UserSalle getCurrentUser() throws SalleException {
        return userRepository.findByEmail(getCurrentUserEmail()).orElseThrow(() -> new SalleException(ErrorCodes.USER_NOT_FOUND));
    }

    public List<Role> getCurrentUserRoles() throws SalleException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return List.of(Role.PARTICIPANT);

        Set<String> authorities = auth.getAuthorities()
                .stream().map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (authorities.contains("ROLE_ADMIN")) {
            return List.of(Role.ADMIN);
        }

        boolean isPD = authorities.stream().anyMatch(a -> a.contains(":PASTORAL_DELEGATE:"));
        if (isPD) return List.of(Role.PASTORAL_DELEGATE);

        boolean isGL = authorities.stream().anyMatch(a -> a.contains(":GROUP_LEADER:"));
        if (isGL) return List.of(Role.GROUP_LEADER);

        boolean isAnimator = authorities.stream().anyMatch(a -> a.contains(":ANIMATOR:"));
        if (isAnimator) return List.of(Role.ANIMATOR);

        return List.of(Role.PARTICIPANT);
    }

    public AuthResponseDto getJwtTokensAfterAuthentication(Authentication authentication, HttpServletResponse response) throws SalleException {
        var user = userInfoRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new SalleException(ErrorCodes.INVALID_CREDENTIALS));

        String accessToken = jwtTokenGenerator.generateAccessToken(authentication);
        String refreshToken = jwtTokenGenerator.generateRefreshToken(authentication);

        saveUserRefreshToken(user, refreshToken);
        setRefreshCookie(response, refreshToken, jwtProps.isCookieSecure());

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .accessTokenExpiry(accessTtlSeconds())
                .userName(user.getEmail())
                .tokenType(TokenType.Bearer)
                .build();
    }

    // AuthService.java
    public Object getAccessTokenUsingRefreshToken(String authorizationHeader, HttpServletResponse response) throws SalleException {

        if (authorizationHeader == null || !authorizationHeader.startsWith(TokenType.Bearer.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid header");
        }
        final String refreshToken = authorizationHeader.substring(7);

        // 1) Validar + extraer subject
        String emailFromJwt;
        try {
            emailFromJwt = jwtTokenGenerator.validateAndExtractSubjectFromRefresh(refreshToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        // 2) Verificar en BD que no está revocado
        var refreshTokenEntity = refreshTokenRepo.findByToken(refreshToken)
                .filter(tokens -> !tokens.isRevoked())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token revoked"));

        UserSalle user = refreshTokenEntity.getUser();
        if (!user.getEmail().equalsIgnoreCase(emailFromJwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token subject mismatch");
        }

        // 3) Construir Authentication con authorities (isAdmin + contextuales)
        List<GrantedAuthority> auths = new java.util.ArrayList<>();
        if (Boolean.TRUE.equals(user.getIsAdmin())) {
            auths.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        var contextual = authorityService.buildContextAuthorities(user.getId());
        contextual.forEach(a -> auths.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(a)));
        Authentication authForTokens =
                new UsernamePasswordAuthenticationToken(user.getEmail(), "N/A", auths);

        // 4) Nuevo access token
        String accessToken = jwtTokenGenerator.generateAccessToken(authForTokens);

        // ----- PASO C: Rotación de refresh -----
        // 5) Revocar el refresh usado
        refreshTokenEntity.setRevoked(true);
        refreshTokenRepo.save(refreshTokenEntity);

        // 6) Emitir nuevo refresh, persistirlo y setear cookie HttpOnly
        String newRefresh = jwtTokenGenerator.generateRefreshToken(authForTokens);
        saveUserRefreshToken(user, newRefresh);
        setRefreshCookie(response, newRefresh, jwtProps.isCookieSecure());

        // 7) Responder con el nuevo access
        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .accessTokenExpiry(accessTtlSeconds())   // <-- en ambos sitios
                .userName(user.getEmail())
                .tokenType(TokenType.Bearer)
                .build();
    }

    private Authentication createAuthenticationObject(UserSalle user) {
        List<String> authStrings;
        try {
            authStrings = authorityService.buildAuthoritiesForUser(user.getId(), user.getIsAdmin());
        } catch (SalleException e) {
            authStrings = List.of();
        }

        var authorities = authStrings.stream()
                .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }

    // AuthService.java (helper)
    private void setRefreshCookie(HttpServletResponse response, String refreshToken, boolean secure) {
        String sameSite = secure ? "None" : "Lax"; // HTTPS -> None; HTTP local -> Lax

        ResponseCookie rc = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(secure)
                .path("/")                 // MUY IMPORTANTE
                .sameSite(sameSite)        // Lax en local HTTP, None en prod HTTPS
                .maxAge(jwtProps.getRefreshTtl()) // o segundos
                .build();

        response.addHeader("Set-Cookie", rc.toString());
    }

    private void saveUserRefreshToken(UserSalle userInfoEntity, String refreshToken) {
        var refreshTokenEntity = RefreshToken.builder()
                .user(userInfoEntity)
                .token(refreshToken)
                .revoked(false)
                .build();
        refreshTokenRepo.save(refreshTokenEntity);
    }

    public UserSalle registerUser(UserRegistrationDto userRegistrationDto,HttpServletResponse httpServletResponse){

        try{
            log.info("[AuthService:registerUser]User Registration Started with :::{}",userRegistrationDto);

            Optional<UserSalle> user = userInfoRepo.findByEmail(userRegistrationDto.userEmail());
            if(user.isPresent()){
                throw new Exception("User Already Exist");
            }

            UserSalle userDetailsEntity = userInfoMapper.convertToEntity(userRegistrationDto);

            UserSalle savedUserDetails = userInfoRepo.save(userDetailsEntity);            
            
            log.info("[AuthService:registerUser] User:{} Successfully registered",savedUserDetails.getEmail());
            return savedUserDetails;


        }catch (Exception e){
            log.error("[AuthService:registerUser]Exception while registering the user due to :"+e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage());
        }
    }

    private int accessTtlSeconds() {
        return (int) jwtProps.getAccessTtl().toSeconds(); // p.ej. 900
    }
}

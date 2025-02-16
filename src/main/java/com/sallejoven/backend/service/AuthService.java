package com.sallejoven.backend.service;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.sallejoven.backend.config.security.JwtTokenGenerator;
import com.sallejoven.backend.mapper.UserInfoMapper;
import com.sallejoven.backend.model.dto.AuthResponseDto;
import com.sallejoven.backend.model.dto.UserRegistrationDto;
import com.sallejoven.backend.model.entity.RefreshToken;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.TokenType;
import com.sallejoven.backend.repository.RefreshTokenRepository;
import com.sallejoven.backend.repository.UserRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userInfoRepo;
    private final UserInfoMapper userInfoMapper;
    private final JwtTokenGenerator jwtTokenGenerator;
    private final RefreshTokenRepository refreshTokenRepo;

    public AuthResponseDto getJwtTokensAfterAuthentication(Authentication authentication, HttpServletResponse response) {
        try
        {
            var userInfoEntity = userInfoRepo.findByEmail(authentication.getName())
                    .orElseThrow(()->{
                        log.error("[AuthService:userSignInAuth] User :{} not found",authentication.getName());
                        return new ResponseStatusException(HttpStatus.NOT_FOUND,"USER NOT FOUND ");});


            String accessToken = jwtTokenGenerator.generateAccessToken(authentication);
            String refreshToken = jwtTokenGenerator.generateRefreshToken(authentication);

            log.info("[AuthService:userSignInAuth] Access token for user:{}, has been generated",userInfoEntity.getEmail());
            saveUserRefreshToken(userInfoEntity,refreshToken);

            creatRefreshTokenCookie(response,refreshToken);

            return  AuthResponseDto.builder()
                    .accessToken(accessToken)
                    .accessTokenExpiry(15 * 60)
                    .userName(userInfoEntity.getEmail())
                    .tokenType(TokenType.Bearer)
                    .build();


        }catch (Exception e){
            log.error("[AuthService:userSignInAuth]Exception while authenticating the user due to :"+e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Please Try Again");
        }
    }

    public Object getAccessTokenUsingRefreshToken(String authorizationHeader) {
 
        if(!authorizationHeader.startsWith(TokenType.Bearer.name())){
            return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Please verify your token type");
        }

        final String refreshToken = authorizationHeader.substring(7);

        var refreshTokenEntity = refreshTokenRepo.findByToken(refreshToken)
                .filter(tokens-> !tokens.isRevoked())
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Refresh token revoked"));

        UserSalle userInfoEntity = refreshTokenEntity.getUser();
        
        Authentication authentication =  createAuthenticationObject(userInfoEntity);

        String accessToken = jwtTokenGenerator.generateAccessToken(authentication);

        return  AuthResponseDto.builder()
                .accessToken(accessToken)
                .accessTokenExpiry(5 * 60)
                .userName(userInfoEntity.getEmail())
                .tokenType(TokenType.Bearer)
                .build();
    }

    private static Authentication createAuthenticationObject(UserSalle userInfoEntity) {
         String username = userInfoEntity.getEmail();
         String password = userInfoEntity.getPassword();
         String roles = userInfoEntity.getRoles();
 
         String[] roleArray = roles.split(",");
         GrantedAuthority[] authorities = Arrays.stream(roleArray)
                 .map(role -> (GrantedAuthority) role::trim)
                 .toArray(GrantedAuthority[]::new);
 
         return new UsernamePasswordAuthenticationToken(username, password, Arrays.asList(authorities));
    }

    private Cookie creatRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie refreshTokenCookie = new Cookie("refresh_token",refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setMaxAge(15 * 24 * 60 * 60 ); // in seconds
        response.addCookie(refreshTokenCookie);
        return refreshTokenCookie;
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
}

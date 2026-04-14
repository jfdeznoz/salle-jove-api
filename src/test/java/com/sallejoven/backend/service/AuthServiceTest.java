package com.sallejoven.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sallejoven.backend.config.security.JwtProperties;
import com.sallejoven.backend.config.security.JwtTokenGenerator;
import com.sallejoven.backend.mapper.UserInfoMapper;
import com.sallejoven.backend.model.entity.RefreshToken;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.repository.RefreshTokenRepository;
import com.sallejoven.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserInfoMapper userInfoMapper;
    @Mock JwtTokenGenerator jwtTokenGenerator;
    @Mock RefreshTokenRepository refreshTokenRepo;
    @Mock AuthorityService authorityService;
    @Mock JwtProperties jwtProps;

    @InjectMocks AuthService authService;

    @Test
    void getJwtTokensAfterAuthentication_savesRefreshTokenWithUuid() {
        UserSalle user = new UserSalle();
        user.setEmail("user@example.com");

        Authentication authentication =
                new UsernamePasswordAuthenticationToken("user@example.com", "N/A");
        HttpServletResponse response = new MockHttpServletResponse();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenGenerator.generateAccessToken(authentication)).thenReturn("access-token");
        when(jwtTokenGenerator.generateRefreshToken(authentication)).thenReturn("refresh-token");
        when(jwtProps.isCookieSecure()).thenReturn(false);
        when(jwtProps.getAccessTtl()).thenReturn(Duration.ofMinutes(30));
        when(jwtProps.getRefreshTtl()).thenReturn(Duration.ofDays(7));
        when(refreshTokenRepo.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.getJwtTokensAfterAuthentication(authentication, response);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepo).save(captor.capture());
        RefreshToken savedToken = captor.getValue();

        assertThat(savedToken.getUuid()).isNotNull();
        assertThat(savedToken.getToken()).isEqualTo("refresh-token");
        assertThat(savedToken.getUser()).isSameAs(user);
        assertThat(savedToken.isRevoked()).isFalse();
    }
}

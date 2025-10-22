package com.sallejoven.backend.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.sallejoven.backend.model.entity.UserSalle;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserInfoConfig implements UserDetails {
    private final UserSalle userInfoEntity;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        boolean isAdmin = Boolean.TRUE.equals(userInfoEntity.getIsAdmin());
        return isAdmin ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN")) : List.of();
    }

    @Override
    public String getPassword() {
        return userInfoEntity.getPassword();
    }

    @Override
    public String getUsername() {
        return userInfoEntity.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
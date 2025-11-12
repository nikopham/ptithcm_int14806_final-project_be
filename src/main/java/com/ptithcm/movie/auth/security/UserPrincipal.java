package com.ptithcm.movie.auth.security;

import com.ptithcm.movie.user.entity.User;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final User user;

    public UUID getId() { return user.getId(); }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        // nếu có role/permission thì map ở đây, tạm thời trả List.of()
        return List.of();
    }
    @Override public String getPassword() { return user.getPasswordHash(); }
    @Override public String getUsername() { return user.getEmail(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked()  { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return user.getEmailVerified(); }
}
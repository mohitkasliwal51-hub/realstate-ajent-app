package com.bhartiyasaas.stayfile.security;

import com.bhartiyasaas.stayfile.entity.Profile;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class SecurityUser implements UserDetails {

    private final Profile profile;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (profile.getRole() == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + profile.getRole().name()));
    }

    @Override
    public String getPassword() {
        return profile.getPassword();
    }

    @Override
    public String getUsername() {
        return profile.getEmail();
    }

    public UUID getProfileId() {
        return profile.getId();
    }

    public UUID getOrganizationId() {
        return profile.getOrganizationId();
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
        return Boolean.TRUE.equals(profile.getIsActive());
    }
}

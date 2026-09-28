package com.bhartiyasaas.stayfile.service.impl;

import com.bhartiyasaas.stayfile.dto.request.LoginRequest;
import com.bhartiyasaas.stayfile.dto.request.RegisterRequest;
import com.bhartiyasaas.stayfile.dto.response.AuthResponse;
import com.bhartiyasaas.stayfile.dto.response.UserProfileResponse;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Profile;
import com.bhartiyasaas.stayfile.entity.enums.UserRole;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.repository.ProfileRepository;
import com.bhartiyasaas.stayfile.security.JwtTokenProvider;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ProfileRepository profileRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Value("${stayfile.jwt.expiration:86400000}")
    private Long jwtExpiration;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (profileRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("An account with email " + request.getEmail() + " already exists.");
        }

        String slug = request.getOrganizationSlug();
        if (slug == null || slug.isBlank()) {
            slug = generateSlug(request.getOrganizationName());
        } else {
            slug = generateSlug(slug);
        }

        if (organizationRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis() % 10000;
        }

        Organization organization = Organization.builder()
                .name(request.getOrganizationName())
                .slug(slug)
                .isActive(true)
                .build();
        organization = organizationRepository.save(organization);

        Profile profile = Profile.builder()
                .organization(organization)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(UserRole.OWNER_ADMIN)
                .isActive(true)
                .build();
        profile = profileRepository.save(profile);

        SecurityUser securityUser = new SecurityUser(profile);
        String token = jwtTokenProvider.generateToken(securityUser);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMs(jwtExpiration)
                .profileId(profile.getId())
                .email(profile.getEmail())
                .fullName(profile.getFullName())
                .role(profile.getRole())
                .organizationId(organization.getId())
                .organizationName(organization.getName())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        Profile profile = securityUser.getProfile();
        String token = jwtTokenProvider.generateToken(securityUser);

        Organization org = profile.getOrganization();
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMs(jwtExpiration)
                .profileId(profile.getId())
                .email(profile.getEmail())
                .fullName(profile.getFullName())
                .role(profile.getRole())
                .organizationId(profile.getOrganizationId())
                .organizationName(org != null ? org.getName() : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(SecurityUser currentUser) {
        Profile profile = profileRepository.findById(currentUser.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with ID: " + currentUser.getProfileId()));

        Organization org = profile.getOrganization();
        return UserProfileResponse.builder()
                .profileId(profile.getId())
                .email(profile.getEmail())
                .fullName(profile.getFullName())
                .phone(profile.getPhone())
                .avatarUrl(profile.getAvatarUrl())
                .role(profile.getRole())
                .organizationId(profile.getOrganizationId())
                .organizationName(org != null ? org.getName() : null)
                .organizationSlug(org != null ? org.getSlug() : null)
                .isActive(profile.getIsActive())
                .build();
    }

    private String generateSlug(String input) {
        if (input == null) return "org-" + System.currentTimeMillis();
        String nowhitespace = input.trim().replaceAll("\\s+", "-");
        String normalized = java.text.Normalizer.normalize(nowhitespace, java.text.Normalizer.Form.NFD);
        String slug = normalized.replaceAll("[^\\w-]", "").toLowerCase(Locale.ENGLISH);
        return slug.replaceAll("-+", "-");
    }
}

package com.bhartiyasaas.stayfile.service.impl;

import com.bhartiyasaas.stayfile.dto.request.LoginRequest;
import com.bhartiyasaas.stayfile.dto.request.RegisterRequest;
import com.bhartiyasaas.stayfile.dto.response.AuthResponse;
import com.bhartiyasaas.stayfile.dto.response.UserProfileResponse;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Profile;
import com.bhartiyasaas.stayfile.entity.BrandingSettings;
import com.bhartiyasaas.stayfile.entity.enums.UserRole;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.repository.ProfileRepository;
import com.bhartiyasaas.stayfile.repository.BrandingSettingsRepository;
import com.bhartiyasaas.stayfile.dto.response.AuthUserResponse;
import com.bhartiyasaas.stayfile.security.JwtTokenProvider;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.AuthService;
import lombok.RequiredArgsConstructor;
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
    private final BrandingSettingsRepository brandingSettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
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

        brandingSettingsRepository.save(BrandingSettings.builder()
            .organization(organization)
            .legalBusinessName(request.getOrganizationName())
            .tradeName(request.getOrganizationName())
            .contactEmail(request.getEmail())
            .contactPhone(request.getPhone())
            .primaryColor("#2563eb")
            .secondaryColor("#1e293b")
            .build());

        Profile profile = Profile.builder()
                .organization(organization)
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
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
                .user(toAuthUser(profile, organization))
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
                .user(toAuthUser(profile, org))
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

    private AuthUserResponse toAuthUser(Profile profile, Organization organization) {
        return AuthUserResponse.builder()
                .id(profile.getId())
                .email(profile.getEmail())
                .fullName(profile.getFullName())
                .role(profile.getRole())
                .organizationId(profile.getOrganizationId())
                .organizationName(organization == null ? null : organization.getName())
                .phone(profile.getPhone())
                .build();
    }
}

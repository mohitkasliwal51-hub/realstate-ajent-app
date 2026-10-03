package com.bhartiyasaas.stayfile.security;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.bhartiyasaas.stayfile.entity.Tenant;
import com.bhartiyasaas.stayfile.entity.enums.UserRole;

@Component
public class TenantAccessService {

    public void validateTenantOwnership(Tenant tenant, String resourceName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser securityUser
                && securityUser.getProfile().getRole() == UserRole.TENANT
                && (tenant == null || tenant.getUser() == null
                || !tenant.getUser().getId().equals(securityUser.getProfileId()))) {
            throw new AccessDeniedException("Access denied: You can only access your own " + resourceName);
        }
    }

    public void validateTenantEmail(String tenantEmail, String resourceName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser securityUser) {
            if (securityUser.getProfile().getRole() == UserRole.TENANT) {
                if (tenantEmail == null || !tenantEmail.equalsIgnoreCase(securityUser.getUsername())) {
                    throw new AccessDeniedException("Access denied: You can only access your own " + resourceName);
                }
            }
        }
    }

    public boolean canAccessTenant(Tenant tenant, String resourceName) {
        try {
            validateTenantOwnership(tenant, resourceName);
            return true;
        } catch (AccessDeniedException e) {
            return false;
        }
    }

    public UUID getCurrentOrganizationId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUser securityUser)) {
            throw new AccessDeniedException("Authentication is required");
        }
        UUID userOrgId = securityUser.getOrganizationId();
        if (userOrgId == null && securityUser.getProfile().getRole() != UserRole.SUPER_ADMIN) {
            throw new AccessDeniedException("Organization is required");
        }
        return userOrgId;
    }

    public void validateUserOrganization() {
        getCurrentOrganizationId();
    }

    public void validateUserOrganization(UUID requestOrganizationId) {
        if (requestOrganizationId == null) {
            throw new AccessDeniedException("Organization is required");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUser securityUser)) {
            throw new AccessDeniedException("Authentication is required");
        }
        if (securityUser.getProfile().getRole() == UserRole.SUPER_ADMIN) {
            return;
        }
        UUID userOrgId = securityUser.getOrganizationId();
        if (userOrgId == null || !userOrgId.equals(requestOrganizationId)) {
            throw new AccessDeniedException("Access denied: You cannot access another organization");
        }
    }
}

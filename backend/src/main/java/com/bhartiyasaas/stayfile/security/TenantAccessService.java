package com.bhartiyasaas.stayfile.security;

import com.bhartiyasaas.stayfile.entity.Tenant;
import com.bhartiyasaas.stayfile.entity.enums.UserRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class TenantAccessService {

    public void validateTenantOwnership(Tenant tenant, String resourceName) {
        validateTenantEmail(tenant == null ? null : tenant.getEmail(), resourceName);
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
}

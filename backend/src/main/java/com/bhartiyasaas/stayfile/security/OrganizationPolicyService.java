package com.bhartiyasaas.stayfile.security;

import com.bhartiyasaas.stayfile.entity.Landlord;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Property;
import com.bhartiyasaas.stayfile.entity.enums.BrokerageFeeType;
import com.bhartiyasaas.stayfile.entity.enums.OrganizationType;
import com.bhartiyasaas.stayfile.exception.BadRequestException;
import com.bhartiyasaas.stayfile.exception.FeatureNotAvailableException;
import org.springframework.stereotype.Service;

@Service
public class OrganizationPolicyService {

    public void requireLandlordManagement(Organization organization) {
        if (organization.getOrganizationType() == OrganizationType.OWNER) {
            throw new FeatureNotAvailableException("Landlord management is not available for direct OWNER organizations");
        }
    }

    public void requirePayoutManagement(Organization organization) {
        if (organization.getOrganizationType() == OrganizationType.OWNER) {
            throw new FeatureNotAvailableException("Landlord payouts are not available for direct OWNER organizations");
        }
    }

    public void validatePropertyLandlord(Organization organization, Landlord landlord) {
        OrganizationType type = organization.getOrganizationType() != null ? organization.getOrganizationType() : OrganizationType.OWNER;
        if (type == OrganizationType.OWNER && landlord != null) {
            throw new BadRequestException("Direct OWNER organizations cannot assign external landlords to properties");
        }
        if (type == OrganizationType.BROKERAGE && landlord == null) {
            throw new BadRequestException("BROKERAGE organizations must assign a landlord to every property");
        }
    }

    public void validateLeaseBrokerageFee(Property property, BrokerageFeeType brokerageFeeType) {
        if (property.getLandlord() == null && brokerageFeeType != null && brokerageFeeType != BrokerageFeeType.NONE) {
            throw new BadRequestException("Brokerage fees cannot be configured for self-owned properties");
        }
    }

    public void validateOrgTypeChange(Organization org, OrganizationType targetType, long landlordCount, long managedPropertyCount, long selfOwnedPropertyCount) {
        OrganizationType currentType = org.getOrganizationType() != null ? org.getOrganizationType() : OrganizationType.OWNER;
        if (currentType == targetType) return;

        if (targetType == OrganizationType.OWNER) {
            if (landlordCount > 0 || managedPropertyCount > 0) {
                throw new BadRequestException("Cannot switch organization type to OWNER while active landlords or managed properties exist");
            }
        } else if (targetType == OrganizationType.BROKERAGE) {
            if (selfOwnedPropertyCount > 0) {
                throw new BadRequestException("Cannot switch organization type to BROKERAGE while self-owned properties exist");
            }
        }
    }
}

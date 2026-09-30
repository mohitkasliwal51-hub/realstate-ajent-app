package com.bhartiyasaas.stayfile.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.dto.request.PropertyLeadCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.PropertyLeadResponse;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Property;
import com.bhartiyasaas.stayfile.entity.PropertyLead;
import com.bhartiyasaas.stayfile.entity.Unit;
import com.bhartiyasaas.stayfile.entity.enums.LeadStatus;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.mapper.PropertyLeadMapper;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.repository.PropertyLeadRepository;
import com.bhartiyasaas.stayfile.repository.PropertyRepository;
import com.bhartiyasaas.stayfile.repository.UnitRepository;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.security.TenantAccessService;
import com.bhartiyasaas.stayfile.service.PropertyLeadService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PropertyLeadServiceImpl implements PropertyLeadService {

    private final PropertyLeadRepository leadRepository;
    private final OrganizationRepository organizationRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final PropertyLeadMapper leadMapper;
    private final TenantAccessService tenantAccessService;

    @Override
    @Transactional
    public PropertyLeadResponse createLead(PropertyLeadCreateRequest request, SecurityUser currentUser) {
        UUID organizationId = currentUser != null ? currentUser.getOrganizationId() : null;

        Property property = null;
        if (request.getPropertyId() != null) {
            property = propertyRepository.findById(request.getPropertyId()).orElse(null);
            if (property != null && organizationId == null) {
                organizationId = property.getOrganization().getId();
            }
        }

        Unit unit = null;
        if (request.getUnitId() != null) {
            unit = unitRepository.findById(request.getUnitId()).orElse(null);
            if (unit != null && organizationId == null) {
                organizationId = unit.getOrganization().getId();
            }
        }

        if (organizationId == null) {
            throw new ResourceNotFoundException("Target organization could not be determined for lead inquiry");
        }

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        PropertyLead lead = leadMapper.toEntity(request);
        lead.setOrganization(org);
        lead.setProperty(property);
        lead.setUnit(unit);
        lead.setStatus(LeadStatus.NEW);

        PropertyLead saved = leadRepository.save(lead);
        return leadMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyLeadResponse> getLeadsByOrganization(SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        List<PropertyLead> leads = leadRepository.findByOrganizationId(organizationId);
        return leadMapper.toResponseList(leads);
    }

    @Override
    @Transactional
    public PropertyLeadResponse updateLeadStatus(UUID leadId, SecurityUser currentUser, LeadStatus status) {
        UUID organizationId = currentUser.getOrganizationId();
        PropertyLead lead = leadRepository.findByIdAndOrganizationId(leadId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        lead.setStatus(status);
        PropertyLead updated = leadRepository.save(lead);
        return leadMapper.toResponse(updated);
    }
}

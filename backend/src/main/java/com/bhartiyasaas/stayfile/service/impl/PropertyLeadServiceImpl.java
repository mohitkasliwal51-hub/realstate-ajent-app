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
    public PropertyLeadResponse createLead(PropertyLeadCreateRequest request) {
        Organization org = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Property property = null;
        if (request.getPropertyId() != null) {
            property = propertyRepository.findByIdAndOrganizationId(request.getPropertyId(), request.getOrganizationId()).orElse(null);
        }

        Unit unit = null;
        if (request.getUnitId() != null) {
            unit = unitRepository.findByIdAndOrganizationId(request.getUnitId(), request.getOrganizationId()).orElse(null);
        }

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
    public List<PropertyLeadResponse> getLeadsByOrganization(UUID organizationId) {
        tenantAccessService.validateUserOrganization(organizationId);
        List<PropertyLead> leads = leadRepository.findByOrganizationId(organizationId);
        return leadMapper.toResponseList(leads);
    }

    @Override
    @Transactional
    public PropertyLeadResponse updateLeadStatus(UUID leadId, UUID organizationId, LeadStatus status) {
        tenantAccessService.validateUserOrganization(organizationId);
        PropertyLead lead = leadRepository.findByIdAndOrganizationId(leadId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        lead.setStatus(status);
        PropertyLead updated = leadRepository.save(lead);
        return leadMapper.toResponse(updated);
    }
}

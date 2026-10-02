package com.bhartiyasaas.stayfile.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.dto.request.LandlordRequest;
import com.bhartiyasaas.stayfile.dto.response.LandlordResponse;
import com.bhartiyasaas.stayfile.entity.Landlord;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.mapper.LandlordMapper;
import com.bhartiyasaas.stayfile.repository.LandlordRepository;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.LandlordService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LandlordServiceImpl implements LandlordService {

    private final LandlordRepository landlordRepository;
    private final OrganizationRepository organizationRepository;
    private final LandlordMapper landlordMapper;

    @Override
    @Transactional
    public LandlordResponse createLandlord(LandlordRequest request, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));

        Landlord landlord = landlordMapper.toEntity(request);
        landlord.setManagingOrganization(organization);
        landlord.setIsActive(true);

        Landlord saved = landlordRepository.save(landlord);
        return landlordMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LandlordResponse getLandlordById(UUID id, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Landlord landlord = landlordRepository.findByIdAndManagingOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord not found with ID: " + id));
        return landlordMapper.toResponse(landlord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LandlordResponse> getLandlordsByOrganization(SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        return landlordMapper.toResponseList(landlordRepository.findByManagingOrganizationId(organizationId));
    }

    @Override
    @Transactional
    public LandlordResponse updateLandlord(UUID id, LandlordRequest request, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Landlord existing = landlordRepository.findByIdAndManagingOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord not found with ID: " + id));

        existing.setLegalName(request.getLegalName());
        existing.setPhone(request.getPhone());
        existing.setEmail(request.getEmail());
        existing.setOwnerType(request.getOwnerType());
        existing.setPan(request.getPan());
        existing.setGstin(request.getGstin());
        existing.setAddress(request.getAddress());
        existing.setBankAccountNumber(request.getBankAccountNumber());
        existing.setBankIfscCode(request.getBankIfscCode());
        existing.setBankName(request.getBankName());
        existing.setAccountHolderName(request.getAccountHolderName());
        existing.setOwnerUpiId(request.getOwnerUpiId());

        Landlord saved = landlordRepository.save(existing);
        return landlordMapper.toResponse(saved);
    }
}

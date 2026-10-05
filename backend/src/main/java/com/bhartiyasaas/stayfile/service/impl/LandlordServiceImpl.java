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
import com.bhartiyasaas.stayfile.security.OrganizationPolicyService;
import com.bhartiyasaas.stayfile.security.PiiEncryptionService;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.LandlordService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LandlordServiceImpl implements LandlordService {

    private final LandlordRepository landlordRepository;
    private final OrganizationRepository organizationRepository;
    private final LandlordMapper landlordMapper;
    private final OrganizationPolicyService organizationPolicyService;
    private final PiiEncryptionService piiEncryptionService;

    @Override
    @Transactional
    public LandlordResponse createLandlord(LandlordRequest request, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));

        organizationPolicyService.requireLandlordManagement(organization);

        Landlord landlord = landlordMapper.toEntity(request);
        landlord.setManagingOrganization(organization);
        landlord.setIsActive(true);

        if (request.getPan() != null && !request.getPan().isBlank()) {
            String trimmedPan = request.getPan().trim().toUpperCase();
            if (trimmedPan.length() >= 4) {
                landlord.setPanLast4(trimmedPan.substring(trimmedPan.length() - 4));
            }
            landlord.setPan(piiEncryptionService.encrypt(trimmedPan));
        }

        if (request.getBankAccountNumber() != null && !request.getBankAccountNumber().isBlank()) {
            String trimmedBank = request.getBankAccountNumber().trim();
            if (trimmedBank.length() >= 4) {
                landlord.setBankAccountLast4(trimmedBank.substring(trimmedBank.length() - 4));
            }
            landlord.setBankAccountNumber(piiEncryptionService.encrypt(trimmedBank));
        }

        Landlord saved = landlordRepository.save(landlord);
        return sanitizeLandlordResponse(saved, currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public LandlordResponse getLandlordById(UUID id, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));
        organizationPolicyService.requireLandlordManagement(organization);

        Landlord landlord = landlordRepository.findByIdAndManagingOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord not found with ID: " + id));
        return sanitizeLandlordResponse(landlord, currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LandlordResponse> getLandlordsByOrganization(SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));
        organizationPolicyService.requireLandlordManagement(organization);

        return landlordRepository.findByManagingOrganizationId(organizationId).stream()
                .map(l -> sanitizeLandlordResponse(l, currentUser))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LandlordResponse updateLandlord(UUID id, LandlordRequest request, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));
        organizationPolicyService.requireLandlordManagement(organization);

        Landlord existing = landlordRepository.findByIdAndManagingOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord not found with ID: " + id));

        existing.setLegalName(request.getLegalName());
        existing.setPhone(request.getPhone());
        existing.setEmail(request.getEmail());
        existing.setOwnerType(request.getOwnerType());
        if (request.getPan() != null && !request.getPan().isBlank()) {
            String trimmedPan = request.getPan().trim().toUpperCase();
            if (!trimmedPan.startsWith("ENC:GCM:v1:")) {
                if (trimmedPan.length() >= 4) {
                    existing.setPanLast4(trimmedPan.substring(trimmedPan.length() - 4));
                }
                existing.setPan(piiEncryptionService.encrypt(trimmedPan));
            }
        }
        existing.setGstin(request.getGstin());
        existing.setAddress(request.getAddress());
        if (request.getBankAccountNumber() != null && !request.getBankAccountNumber().isBlank()) {
            String trimmedBank = request.getBankAccountNumber().trim();
            if (!trimmedBank.startsWith("ENC:GCM:v1:")) {
                if (trimmedBank.length() >= 4) {
                    existing.setBankAccountLast4(trimmedBank.substring(trimmedBank.length() - 4));
                }
                existing.setBankAccountNumber(piiEncryptionService.encrypt(trimmedBank));
            }
        }
        existing.setBankIfscCode(request.getBankIfscCode());
        existing.setBankName(request.getBankName());
        existing.setAccountHolderName(request.getAccountHolderName());
        existing.setOwnerUpiId(request.getOwnerUpiId());

        Landlord saved = landlordRepository.save(existing);
        return sanitizeLandlordResponse(saved, currentUser);
    }

    @Override
    @Transactional
    public void deleteLandlord(UUID id, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));
        organizationPolicyService.requireLandlordManagement(organization);

        Landlord landlord = landlordRepository.findByIdAndManagingOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord not found with ID: " + id));
        landlord.setIsActive(false);
        landlordRepository.save(landlord);
    }

    private LandlordResponse sanitizeLandlordResponse(Landlord landlord, SecurityUser currentUser) {
        if (landlord == null) return null;
        LandlordResponse response = landlordMapper.toResponse(landlord);
        boolean isAdmin = currentUser != null && currentUser.getRole() != null 
                && (currentUser.getRole().name().equals("SUPER_ADMIN") || currentUser.getRole().name().equals("ADMIN"));

        String rawPan = landlord.getPan();
        if (rawPan != null && !rawPan.isBlank()) {
            try {
                rawPan = piiEncryptionService.decrypt(rawPan);
            } catch (Exception ignored) {}
        }

        String rawBank = landlord.getBankAccountNumber();
        if (rawBank != null && !rawBank.isBlank()) {
            try {
                rawBank = piiEncryptionService.decrypt(rawBank);
            } catch (Exception ignored) {}
        }

        String panLast4 = landlord.getPanLast4();
        if ((panLast4 == null || panLast4.isBlank()) && rawPan != null && rawPan.length() >= 4) {
            panLast4 = rawPan.substring(rawPan.length() - 4).toUpperCase();
        }
        response.setPanLast4(panLast4);

        String bankLast4 = landlord.getBankAccountLast4();
        if ((bankLast4 == null || bankLast4.isBlank()) && rawBank != null && rawBank.length() >= 4) {
            bankLast4 = rawBank.substring(rawBank.length() - 4);
        }
        response.setBankAccountLast4(bankLast4);

        if (isAdmin) {
            response.setPan(rawPan);
            response.setBankAccountNumber(rawBank);
        } else {
            if (panLast4 != null && !panLast4.isBlank()) {
                response.setPan("XXXXXX" + panLast4);
            } else {
                response.setPan(null);
            }
            if (bankLast4 != null && !bankLast4.isBlank()) {
                response.setBankAccountNumber("XXXX-XXXX-" + bankLast4);
            } else {
                response.setBankAccountNumber(null);
            }
        }
        return response;
    }
}

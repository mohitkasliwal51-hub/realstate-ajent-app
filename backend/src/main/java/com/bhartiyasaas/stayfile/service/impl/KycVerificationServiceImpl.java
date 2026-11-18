package com.bhartiyasaas.stayfile.service.impl;

import com.bhartiyasaas.stayfile.dto.request.DigiLockerInitiateRequest;
import com.bhartiyasaas.stayfile.dto.request.PanVerificationRequest;
import com.bhartiyasaas.stayfile.dto.response.AadhaarMaskingResponse;
import com.bhartiyasaas.stayfile.dto.response.DigiLockerInitiateResponse;
import com.bhartiyasaas.stayfile.dto.response.DigiLockerStatusResponse;
import com.bhartiyasaas.stayfile.dto.response.DigiLockerVerificationCompleteResponse;
import com.bhartiyasaas.stayfile.dto.response.FileUploadResponse;
import com.bhartiyasaas.stayfile.dto.response.PanVerificationResponse;
import com.bhartiyasaas.stayfile.entity.Landlord;
import com.bhartiyasaas.stayfile.entity.Tenant;
import com.bhartiyasaas.stayfile.entity.enums.FileCategory;
import com.bhartiyasaas.stayfile.entity.enums.UserRole;
import com.bhartiyasaas.stayfile.entity.enums.VerificationStatus;
import com.bhartiyasaas.stayfile.exception.BadRequestException;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.integration.client.CashfreeHttpClient;
import com.bhartiyasaas.stayfile.integration.dto.cashfree.CashfreeAadhaarMaskingResponse;
import com.bhartiyasaas.stayfile.integration.dto.cashfree.CashfreeDigiLockerAadhaarDocumentResponse;
import com.bhartiyasaas.stayfile.integration.dto.cashfree.CashfreeDigiLockerCreateUrlRequest;
import com.bhartiyasaas.stayfile.integration.dto.cashfree.CashfreeDigiLockerCreateUrlResponse;
import com.bhartiyasaas.stayfile.integration.dto.cashfree.CashfreeDigiLockerStatusResponse;
import com.bhartiyasaas.stayfile.integration.dto.cashfree.CashfreePanResponse;
import com.bhartiyasaas.stayfile.integration.dto.cashfree.CashfreePanVerifyRequest;
import com.bhartiyasaas.stayfile.repository.LandlordRepository;
import com.bhartiyasaas.stayfile.repository.TenantRepository;
import com.bhartiyasaas.stayfile.security.PiiEncryptionService;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.security.TenantAccessService;
import com.bhartiyasaas.stayfile.service.KycVerificationService;
import com.bhartiyasaas.stayfile.service.StorageService;
import com.bhartiyasaas.stayfile.service.storage.ByteArrayMultipartFile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KycVerificationServiceImpl implements KycVerificationService {

    private final CashfreeHttpClient cashfreeHttpClient;
    private final TenantRepository tenantRepository;
    private final LandlordRepository landlordRepository;
    private final PiiEncryptionService piiEncryptionService;
    private final TenantAccessService tenantAccessService;
    private final StorageService storageService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PanVerificationResponse verifyPan(PanVerificationRequest request, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        if (organizationId == null && currentUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new AccessDeniedException("Organization context is required for KYC verification");
        }

        String rawPan = request.getPan().trim().toUpperCase();
        String name = request.getName() != null && !request.getName().trim().isEmpty() ? request.getName().trim() : null;
        String panLast4 = rawPan.substring(rawPan.length() - 4);

        // Pre-validate target entity existence and access control BEFORE making external API calls
        Tenant targetTenant = null;
        Landlord targetLandlord = null;

        if (currentUser.getRole() == UserRole.TENANT) {
            if (request.getLandlordId() != null) {
                throw new AccessDeniedException("Tenants are not permitted to verify landlord KYC");
            }
            if (request.getTenantId() != null) {
                targetTenant = tenantRepository.findByIdAndOrganizationId(request.getTenantId(), organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + request.getTenantId()));
                tenantAccessService.validateTenantOwnership(targetTenant, "tenant KYC");
            } else {
                targetTenant = tenantRepository.findByUserIdAndOrganizationId(currentUser.getId(), organizationId)
                        .orElse(null);
            }
        } else {
            if (request.getTenantId() != null) {
                targetTenant = tenantRepository.findByIdAndOrganizationId(request.getTenantId(), organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + request.getTenantId()));
            }
            if (request.getLandlordId() != null) {
                targetLandlord = landlordRepository.findByIdAndManagingOrganizationId(request.getLandlordId(), organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Landlord not found with ID: " + request.getLandlordId()));
            }
        }

        // Call Cashfree Sync PAN Verification API
        CashfreePanVerifyRequest cfRequest = CashfreePanVerifyRequest.builder()
                .pan(rawPan)
                .name(name)
                .build();

        log.info("Initiating PAN verification for last4: ****{} in org: {}", panLast4, organizationId);
        CashfreePanResponse cfResponse = cashfreeHttpClient.post(
                "/pan",
                cfRequest,
                CashfreePanResponse.class,
                organizationId
        );

        boolean isValid = Boolean.TRUE.equals(cfResponse.getValid());

        // Structured verification audit metadata
        Map<String, Object> panMeta = new HashMap<>();
        panMeta.put("reference_id", cfResponse.getReferenceIdAsString());
        panMeta.put("registered_name", cfResponse.getRegisteredName());
        panMeta.put("type", cfResponse.getType());
        panMeta.put("valid", isValid);
        panMeta.put("name_match_score", cfResponse.getNameMatchScoreAsString());
        panMeta.put("name_match_result", cfResponse.getNameMatchResult());
        panMeta.put("aadhaar_seeding_status", cfResponse.getAadhaarSeedingStatus());
        panMeta.put("pan_status", cfResponse.getPanStatus());
        panMeta.put("pan_status_code", cfResponse.getPanStatusCode());
        panMeta.put("pan_status_desc", cfResponse.getPanStatusDesc());
        panMeta.put("verified_at", OffsetDateTime.now().toString());

        // Update Tenant if linked
        if (targetTenant != null) {
            targetTenant.setIdProofType("PAN");
            targetTenant.setIdProofLast4("XXXXXX" + panLast4);
            targetTenant.setIdProofNumber(piiEncryptionService.encrypt(rawPan));
            targetTenant.setKycStatus(isValid ? VerificationStatus.VERIFIED : VerificationStatus.REJECTED);
            targetTenant.setMetadata(mergeMetadata(targetTenant.getMetadata(), "pan_verification", panMeta));
            tenantRepository.save(targetTenant);
            log.info("Updated Tenant ID: {} KYC status to {} based on PAN verification", targetTenant.getId(), targetTenant.getKycStatus());
        }

        // Update Landlord if linked
        if (targetLandlord != null) {
            targetLandlord.setPanLast4(panLast4);
            targetLandlord.setPan(piiEncryptionService.encrypt(rawPan));
            targetLandlord.setMetadata(mergeMetadata(targetLandlord.getMetadata(), "pan_verification", panMeta));
            landlordRepository.save(targetLandlord);
            log.info("Updated Landlord ID: {} PAN verification details", targetLandlord.getId());
        }

        return PanVerificationResponse.builder()
                .valid(isValid)
                .panLast4(panLast4)
                .registeredName(cfResponse.getRegisteredName())
                .nameProvided(cfResponse.getNameProvided() != null ? cfResponse.getNameProvided() : name)
                .entityType(cfResponse.getType())
                .nameMatchScore(cfResponse.getNameMatchScoreAsString())
                .nameMatchResult(cfResponse.getNameMatchResult())
                .aadhaarSeedingStatus(cfResponse.getAadhaarSeedingStatus())
                .panStatus(cfResponse.getPanStatus())
                .panStatusCode(cfResponse.getPanStatusCode())
                .panStatusDescription(cfResponse.getPanStatusDesc())
                .referenceId(cfResponse.getReferenceIdAsString())
                .message(cfResponse.getMessage() != null ? cfResponse.getMessage() : (isValid ? "PAN is valid" : "PAN is invalid"))
                .linkedToTenant(targetTenant != null)
                .linkedToLandlord(targetLandlord != null)
                .build();
    }

    @Override
    @Transactional
    public AadhaarMaskingResponse maskAadhaar(MultipartFile file, UUID tenantId, SecurityUser currentUser) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Aadhaar card image or PDF file is required");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BadRequestException("Aadhaar card file size exceeds the 10 MB limit");
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "aadhaar.jpg";
        String lowerFilename = filename.toLowerCase();
        boolean isValidExt = lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")
                || lowerFilename.endsWith(".png") || lowerFilename.endsWith(".pdf");
        if (!isValidExt) {
            throw new BadRequestException("Please upload a valid Aadhaar file format (JPEG, JPG, PNG, or PDF)");
        }

        UUID organizationId = currentUser.getOrganizationId();
        if (organizationId == null && currentUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new AccessDeniedException("Organization context is required for KYC verification");
        }

        Tenant targetTenant = null;
        if (currentUser.getRole() == UserRole.TENANT) {
            if (tenantId != null) {
                targetTenant = tenantRepository.findByIdAndOrganizationId(tenantId, organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + tenantId));
                tenantAccessService.validateTenantOwnership(targetTenant, "tenant KYC");
            } else {
                targetTenant = tenantRepository.findByUserIdAndOrganizationId(currentUser.getId(), organizationId)
                        .orElse(null);
            }
        } else {
            if (tenantId != null) {
                targetTenant = tenantRepository.findByIdAndOrganizationId(tenantId, organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + tenantId));
            }
        }

        String verificationId = "sf_adh_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception ex) {
            throw new BadRequestException("Unable to read uploaded Aadhaar file: " + ex.getMessage());
        }

        ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        MultiValueMap<String, Object> bodyParts = new LinkedMultiValueMap<>();
        bodyParts.add("image", fileResource);
        bodyParts.add("verification_id", verificationId);

        String auditSummary = String.format("{\"verification_id\":\"%s\",\"filename\":\"%s\",\"size\":%d,\"content_type\":\"%s\"}",
                verificationId, filename, file.getSize(), file.getContentType());

        log.info("Sending Aadhaar masking request verification_id: {} for org: {}", verificationId, organizationId);
        CashfreeAadhaarMaskingResponse cfResponse = cashfreeHttpClient.postMultipart(
                "/aadhaar-masking",
                bodyParts,
                auditSummary,
                CashfreeAadhaarMaskingResponse.class,
                organizationId
        );

        boolean isValid = "VALID".equalsIgnoreCase(cfResponse.getStatus());
        String maskedDocUrl = cfResponse.getImageLink();

        // Download and persist masked Aadhaar to StayFile storage so it remains permanently secure & accessible
        if (isValid && cfResponse.getImageLink() != null && !cfResponse.getImageLink().isBlank()) {
            try {
                byte[] maskedBytes = restClient.get()
                        .uri(cfResponse.getImageLink())
                        .retrieve()
                        .body(byte[].class);

                if (maskedBytes != null && maskedBytes.length > 0) {
                    String maskedFilename = "masked_" + filename;
                    MultipartFile maskedFile = new ByteArrayMultipartFile(
                            maskedBytes,
                            "file",
                            maskedFilename,
                            file.getContentType() != null ? file.getContentType() : "image/jpeg"
                    );
                    FileUploadResponse uploadResponse = storageService.storeFile(maskedFile, FileCategory.TENANT_KYC, organizationId);
                    maskedDocUrl = uploadResponse.getUrl();
                    log.info("Successfully persisted masked Aadhaar to StayFile storage: {}", maskedDocUrl);
                }
            } catch (Exception ex) {
                log.warn("Could not download/persist masked Aadhaar from Cashfree link, retaining original link: {}", ex.getMessage());
            }
        }

        if (targetTenant != null) {
            Map<String, Object> aadhaarMeta = new HashMap<>();
            aadhaarMeta.put("reference_id", cfResponse.getReferenceIdAsString());
            aadhaarMeta.put("verification_id", cfResponse.getVerificationId());
            aadhaarMeta.put("status", cfResponse.getStatus());
            aadhaarMeta.put("masked_url", maskedDocUrl);
            aadhaarMeta.put("masked_at", OffsetDateTime.now().toString());

            targetTenant.setIdProofType("Aadhaar");
            if (isValid && maskedDocUrl != null) {
                targetTenant.setIdProofDocumentUrl(maskedDocUrl);
            }
            targetTenant.setMetadata(mergeMetadata(targetTenant.getMetadata(), "aadhaar_masking", aadhaarMeta));
            tenantRepository.save(targetTenant);
            log.info("Updated Tenant ID: {} with masked Aadhaar details", targetTenant.getId());
        }

        String userMessage = isValid ? "Aadhaar masked and secured successfully"
                : "The uploaded file could not be recognized as a valid Aadhaar card. Please upload a clear copy.";

        return AadhaarMaskingResponse.builder()
                .valid(isValid)
                .status(cfResponse.getStatus())
                .referenceId(cfResponse.getReferenceIdAsString())
                .verificationId(cfResponse.getVerificationId())
                .maskedDocumentUrl(maskedDocUrl)
                .message(userMessage)
                .linkedToTenant(targetTenant != null)
                .tenantId(targetTenant != null ? targetTenant.getId() : null)
                .build();
    }

    @Override
    @Transactional
    public DigiLockerInitiateResponse initiateDigiLocker(DigiLockerInitiateRequest request, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        if (organizationId == null && currentUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new AccessDeniedException("Organization context is required for KYC verification");
        }

        final UUID requestedTenantId = request.getTenantId();
        Tenant targetTenant = null;

        if (currentUser.getRole() == UserRole.TENANT) {
            if (requestedTenantId != null) {
                targetTenant = tenantRepository.findByIdAndOrganizationId(requestedTenantId, organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + requestedTenantId));
                tenantAccessService.validateTenantOwnership(targetTenant, "tenant KYC");
            } else {
                targetTenant = tenantRepository.findByUserIdAndOrganizationId(currentUser.getId(), organizationId)
                        .orElse(null);
            }
        } else {
            if (requestedTenantId != null) {
                targetTenant = tenantRepository.findByIdAndOrganizationId(requestedTenantId, organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + requestedTenantId));
            }
        }

        final UUID resolvedTenantId = targetTenant != null ? targetTenant.getId() : requestedTenantId;

        String verificationId = "sf_dgl_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String redirectUrl = request.getRedirectUrl() != null && !request.getRedirectUrl().isBlank()
                ? request.getRedirectUrl().trim()
                : "https://stayfile.in/kyc/callback";
        String userFlow = request.getUserFlow() != null && !request.getUserFlow().isBlank()
                ? request.getUserFlow().trim().toLowerCase()
                : "signup";

        CashfreeDigiLockerCreateUrlRequest cfRequest = CashfreeDigiLockerCreateUrlRequest.builder()
                .verificationId(verificationId)
                .documentRequested(List.of("AADHAAR"))
                .redirectUrl(redirectUrl)
                .userFlow(userFlow)
                .purpose("Tenancy KYC Verification")
                .companyName("StayFile")
                .build();

        log.info("Initiating DigiLocker consent link verification_id: {} in org: {}", verificationId, organizationId);
        CashfreeDigiLockerCreateUrlResponse cfResponse = cashfreeHttpClient.post(
                "/digilocker",
                cfRequest,
                CashfreeDigiLockerCreateUrlResponse.class,
                organizationId
        );

        if (targetTenant != null) {
            Map<String, Object> sessionMeta = new HashMap<>();
            sessionMeta.put("verification_id", verificationId);
            sessionMeta.put("reference_id", cfResponse.getReferenceIdAsString());
            sessionMeta.put("status", cfResponse.getStatus());
            sessionMeta.put("initiated_at", OffsetDateTime.now().toString());

            targetTenant.setMetadata(mergeMetadata(targetTenant.getMetadata(), "digilocker_session", sessionMeta));
            tenantRepository.save(targetTenant);
        }

        return DigiLockerInitiateResponse.builder()
                .verificationId(verificationId)
                .referenceId(cfResponse.getReferenceIdAsString())
                .consentUrl(cfResponse.getUrl())
                .status(cfResponse.getStatus())
                .expiresInMinutes(10)
                .tenantId(resolvedTenantId)
                .message("DigiLocker consent URL generated. Please complete Aadhaar verification within 10 minutes.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DigiLockerStatusResponse getDigiLockerStatus(String verificationId, SecurityUser currentUser) {
        if (verificationId == null || verificationId.isBlank()) {
            throw new BadRequestException("Verification ID is required");
        }
        UUID organizationId = currentUser.getOrganizationId();
        CashfreeDigiLockerStatusResponse statusResponse = cashfreeHttpClient.get(
                "/digilocker/status",
                Map.of("verification_id", verificationId.trim()),
                CashfreeDigiLockerStatusResponse.class,
                organizationId
        );

        String rawStatus = statusResponse != null && statusResponse.getStatus() != null
                ? statusResponse.getStatus().trim().toUpperCase()
                : "UNKNOWN";

        boolean authenticated = "AUTHENTICATED".equalsIgnoreCase(rawStatus);
        boolean expired = "EXPIRED".equalsIgnoreCase(rawStatus);
        boolean consentDenied = "CONSENT_DENIED".equalsIgnoreCase(rawStatus);
        boolean isPending = "PENDING".equalsIgnoreCase(rawStatus) || "INITIATED".equalsIgnoreCase(rawStatus);

        String message;
        String actionRequired;

        if (authenticated) {
            message = "User has successfully authenticated and provided DigiLocker consent. Ready to retrieve Aadhaar records.";
            actionRequired = "PROCEED_TO_COMPLETE";
        } else if (isPending) {
            message = "DigiLocker session is pending. Waiting for user to complete login and Aadhaar OTP verification.";
            actionRequired = "WAIT_FOR_USER_OTP";
        } else if (expired) {
            message = "DigiLocker session has expired (10-minute validity window elapsed). Please initiate a new session.";
            actionRequired = "REINITIATE_SESSION";
        } else if (consentDenied) {
            message = "User rejected the verification request (consent denied on DigiLocker).";
            actionRequired = "REINITIATE_SESSION";
        } else {
            message = "DigiLocker session status: " + rawStatus;
            actionRequired = "CHECK_STATUS";
        }

        return DigiLockerStatusResponse.builder()
                .verificationId(verificationId)
                .referenceId(statusResponse != null ? statusResponse.getReferenceIdAsString() : null)
                .status(rawStatus)
                .message(message)
                .authenticated(authenticated)
                .canComplete(authenticated)
                .expired(expired)
                .consentDenied(consentDenied)
                .documentConsentValidity(statusResponse != null ? statusResponse.getDocumentConsentValidity() : null)
                .actionRequired(actionRequired)
                .build();
    }

    @Override
    @Transactional
    public DigiLockerVerificationCompleteResponse completeDigiLocker(String verificationId, UUID tenantId, SecurityUser currentUser) {
        if (verificationId == null || verificationId.isBlank()) {
            throw new BadRequestException("Verification ID is required");
        }
        UUID organizationId = currentUser.getOrganizationId();
        if (organizationId == null && currentUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new AccessDeniedException("Organization context is required for KYC verification");
        }

        Tenant targetTenant = null;
        if (currentUser.getRole() == UserRole.TENANT) {
            if (tenantId != null) {
                targetTenant = tenantRepository.findByIdAndOrganizationId(tenantId, organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + tenantId));
                tenantAccessService.validateTenantOwnership(targetTenant, "tenant KYC");
            } else {
                targetTenant = tenantRepository.findByUserIdAndOrganizationId(currentUser.getId(), organizationId)
                        .orElse(null);
            }
        } else {
            if (tenantId != null) {
                targetTenant = tenantRepository.findByIdAndOrganizationId(tenantId, organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + tenantId));
            }
        }

        // Edge Case 1: Check session status first to prevent premature document retrieval
        CashfreeDigiLockerStatusResponse statusResponse = cashfreeHttpClient.get(
                "/digilocker/status",
                Map.of("verification_id", verificationId.trim()),
                CashfreeDigiLockerStatusResponse.class,
                organizationId
        );

        String rawStatus = statusResponse != null && statusResponse.getStatus() != null
                ? statusResponse.getStatus().trim().toUpperCase()
                : "UNKNOWN";

        if (!"AUTHENTICATED".equalsIgnoreCase(rawStatus)) {
            String rejectionMsg;
            if ("PENDING".equalsIgnoreCase(rawStatus) || "INITIATED".equalsIgnoreCase(rawStatus)) {
                rejectionMsg = "DigiLocker session is still pending. The user has not completed Aadhaar authentication yet.";
            } else if ("EXPIRED".equalsIgnoreCase(rawStatus)) {
                rejectionMsg = "DigiLocker verification session has expired. Please initiate a new verification link.";
            } else if ("CONSENT_DENIED".equalsIgnoreCase(rawStatus)) {
                rejectionMsg = "DigiLocker verification was rejected because the user denied document consent.";
            } else {
                rejectionMsg = "DigiLocker session is not authenticated (Status: " + rawStatus + ").";
            }

            if (targetTenant != null && ("CONSENT_DENIED".equalsIgnoreCase(rawStatus) || "EXPIRED".equalsIgnoreCase(rawStatus))) {
                Map<String, Object> sessionMeta = new HashMap<>();
                sessionMeta.put("verification_id", verificationId);
                sessionMeta.put("status", rawStatus);
                sessionMeta.put("updated_at", OffsetDateTime.now().toString());
                targetTenant.setMetadata(mergeMetadata(targetTenant.getMetadata(), "digilocker_verification", sessionMeta));
                if ("CONSENT_DENIED".equalsIgnoreCase(rawStatus)) {
                    targetTenant.setKycStatus(VerificationStatus.REJECTED);
                }
                tenantRepository.save(targetTenant);
            }

            return DigiLockerVerificationCompleteResponse.builder()
                    .verified(false)
                    .status(rawStatus)
                    .tenantId(targetTenant != null ? targetTenant.getId() : null)
                    .verificationId(verificationId)
                    .referenceId(statusResponse != null ? statusResponse.getReferenceIdAsString() : null)
                    .message(rejectionMsg)
                    .build();
        }

        log.info("Fetching verified DigiLocker Aadhaar document for verification_id: {} in org: {}", verificationId, organizationId);
        CashfreeDigiLockerAadhaarDocumentResponse docResponse = cashfreeHttpClient.get(
                "/digilocker/document/AADHAAR",
                Map.of("verification_id", verificationId.trim()),
                CashfreeDigiLockerAadhaarDocumentResponse.class,
                organizationId
        );

        boolean isValid = "VALID".equalsIgnoreCase(docResponse.getStatus());

        // Edge Case 2: Document missing or invalid in DigiLocker (e.g. Aadhaar unlinked)
        if (!isValid) {
            String failMsg = "Aadhaar document could not be retrieved or is not valid in DigiLocker (Status: " + docResponse.getStatus() + ").";
            if (targetTenant != null) {
                targetTenant.setKycStatus(VerificationStatus.REJECTED);
                Map<String, Object> dlFailMeta = new HashMap<>();
                dlFailMeta.put("verification_id", verificationId);
                dlFailMeta.put("status", docResponse.getStatus());
                dlFailMeta.put("updated_at", OffsetDateTime.now().toString());
                targetTenant.setMetadata(mergeMetadata(targetTenant.getMetadata(), "digilocker_verification", dlFailMeta));
                tenantRepository.save(targetTenant);
            }
            return DigiLockerVerificationCompleteResponse.builder()
                    .verified(false)
                    .status(docResponse.getStatus())
                    .tenantId(targetTenant != null ? targetTenant.getId() : null)
                    .verificationId(verificationId)
                    .referenceId(docResponse.getReferenceIdAsString())
                    .message(failMsg)
                    .build();
        }

        String photoUrl = null;

        // Edge Case 3: Resilient official UIDAI photograph extraction & storage
        if (docResponse.getImage() != null && !docResponse.getImage().isBlank()) {
            try {
                byte[] photoBytes = Base64.getDecoder().decode(docResponse.getImage().trim());
                if (photoBytes.length > 0) {
                    MultipartFile photoFile = new ByteArrayMultipartFile(
                            photoBytes,
                            "photo",
                            "uidai_photo_" + verificationId + ".jpg",
                            "image/jpeg"
                    );
                    FileUploadResponse uploadResponse = storageService.storeFile(photoFile, FileCategory.TENANT_KYC, organizationId);
                    photoUrl = uploadResponse.getUrl();
                }
            } catch (Exception ex) {
                log.warn("Could not decode or store UIDAI photo from DigiLocker: {}", ex.getMessage());
            }
        }

        String formattedAddress = docResponse.getFormattedAddress();

        if (targetTenant != null) {
            targetTenant.setIdProofType("Aadhaar");
            String uidLast4 = docResponse.getUidLast4();
            if (uidLast4 != null) {
                targetTenant.setIdProofLast4("XXXX-XXXX-" + uidLast4);
            }
            if (docResponse.getName() != null && !docResponse.getName().isBlank()) {
                targetTenant.setFullName(docResponse.getName());
            }
            if (formattedAddress != null && !formattedAddress.isBlank()) {
                targetTenant.setPermanentAddress(formattedAddress);
            }
            if (photoUrl != null) {
                targetTenant.setIdProofDocumentUrl(photoUrl);
            }
            targetTenant.setKycStatus(VerificationStatus.VERIFIED);

            Map<String, Object> dlMeta = new HashMap<>();
            dlMeta.put("verification_id", verificationId);
            dlMeta.put("reference_id", docResponse.getReferenceIdAsString());
            dlMeta.put("care_of", docResponse.getCareOf());
            dlMeta.put("dob", docResponse.getDob());
            dlMeta.put("gender", docResponse.getGender());
            dlMeta.put("masked_uid", docResponse.getUid());
            dlMeta.put("status", "AUTHENTICATED");
            dlMeta.put("photo_url", photoUrl);
            dlMeta.put("verified_at", OffsetDateTime.now().toString());

            targetTenant.setMetadata(mergeMetadata(targetTenant.getMetadata(), "digilocker_verification", dlMeta));
            tenantRepository.save(targetTenant);
            log.info("Tenant ID: {} marked VERIFIED via DigiLocker", targetTenant.getId());
        }

        return DigiLockerVerificationCompleteResponse.builder()
                .verified(true)
                .status(docResponse.getStatus())
                .tenantId(targetTenant != null ? targetTenant.getId() : null)
                .legalName(docResponse.getName())
                .maskedAadhaar(docResponse.getUid())
                .dob(docResponse.getDob())
                .gender(docResponse.getGender())
                .careOf(docResponse.getCareOf())
                .permanentAddress(formattedAddress)
                .photoDocumentUrl(photoUrl)
                .verificationId(verificationId)
                .referenceId(docResponse.getReferenceIdAsString())
                .message("Aadhaar verified successfully through DigiLocker")
                .build();
    }

    private String mergeMetadata(String existingMetadata, String key, Object value) {
        Map<String, Object> map;
        if (existingMetadata != null && !existingMetadata.isBlank()) {
            try {
                map = objectMapper.readValue(existingMetadata, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("Could not deserialize existing metadata json, resetting map: {}", e.getMessage());
                map = new HashMap<>();
            }
        } else {
            map = new HashMap<>();
        }
        map.put(key, value);
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Failed to serialize metadata map: {}", e.getMessage());
            return "{}";
        }
    }
}

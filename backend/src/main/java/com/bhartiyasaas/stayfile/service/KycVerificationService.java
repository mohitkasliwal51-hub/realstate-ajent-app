package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.dto.request.PanVerificationRequest;
import com.bhartiyasaas.stayfile.dto.response.PanVerificationResponse;
import com.bhartiyasaas.stayfile.security.SecurityUser;

import com.bhartiyasaas.stayfile.dto.response.AadhaarMaskingResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

public interface KycVerificationService {

    /**
     * Verifies a PAN synchronously with Cashfree Secure ID.
     * Optionally links and updates verification status for a Tenant or Landlord.
     * All transactions are securely audited in integration_logs.
     *
     * @param request PAN verification payload
     * @param currentUser Authenticated security user
     * @return PanVerificationResponse containing match scores and validity status
     */
    PanVerificationResponse verifyPan(PanVerificationRequest request, SecurityUser currentUser);

    /**
     * Masks an uploaded physical or e-Aadhaar card document (PDF or image)
     * using Cashfree Aadhaar Masking API.
     * Masks the first 8 digits and disables QR codes to ensure full UIDAI compliance.
     * Stored masked document is attached to the tenant profile.
     *
     * @param file Uploaded Aadhaar image or PDF (JPEG, JPG, PNG, PDF <= 10MB)
     * @param tenantId Optional target tenant UUID to link
     * @param currentUser Authenticated security user
     * @return AadhaarMaskingResponse with masked image URL and validity status
     */
    AadhaarMaskingResponse maskAadhaar(MultipartFile file, UUID tenantId, SecurityUser currentUser);

    /**
     * Initiates a DigiLocker consent session to verify Aadhaar via UIDAI OTP.
     * Generates a 10-minute time-sensitive consent link for the tenant to enter Aadhaar & OTP.
     *
     * @param request Initiate request containing optional tenantId and redirectUrl
     * @param currentUser Authenticated security user
     * @return DigiLockerInitiateResponse containing the consent URL
     */
    com.bhartiyasaas.stayfile.dto.response.DigiLockerInitiateResponse initiateDigiLocker(
            com.bhartiyasaas.stayfile.dto.request.DigiLockerInitiateRequest request,
            SecurityUser currentUser
    );

    /**
     * Checks current status of a DigiLocker session (PENDING, AUTHENTICATED, EXPIRED, CONSENT_DENIED).
     *
     * @param verificationId Session identifier
     * @param currentUser Authenticated security user
     * @return DigiLockerStatusResponse with status details and next action guidance
     */
    com.bhartiyasaas.stayfile.dto.response.DigiLockerStatusResponse getDigiLockerStatus(
            String verificationId,
            SecurityUser currentUser
    );

    /**
     * Completes DigiLocker verification after user OTP consent.
     * Retrieves official UIDAI demographic records (Name, Masked Aadhaar, DOB, Gender, Address, Photo),
     * auto-updates tenant profile, stores official photo, and marks KYC status as VERIFIED.
     *
     * @param verificationId Completed session verification ID
     * @param tenantId Target tenant UUID
     * @param currentUser Authenticated security user
     * @return DigiLockerVerificationCompleteResponse with full verified details
     */
    com.bhartiyasaas.stayfile.dto.response.DigiLockerVerificationCompleteResponse completeDigiLocker(
            String verificationId,
            UUID tenantId,
            SecurityUser currentUser
    );
}

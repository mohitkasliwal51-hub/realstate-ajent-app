package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.dto.request.BankIfscLookupRequest;
import com.bhartiyasaas.stayfile.dto.request.BankRpdInitiateRequest;
import com.bhartiyasaas.stayfile.dto.request.BankSyncVerificationRequest;
import com.bhartiyasaas.stayfile.dto.request.BankUpiVerificationRequest;
import com.bhartiyasaas.stayfile.dto.response.BankIfscLookupResponse;
import com.bhartiyasaas.stayfile.dto.response.BankRpdInitiateResponse;
import com.bhartiyasaas.stayfile.dto.response.BankVerificationResultResponse;
import com.bhartiyasaas.stayfile.security.SecurityUser;

import java.util.UUID;

public interface BankVerificationService {

    BankRpdInitiateResponse initiateReversePennyDrop(BankRpdInitiateRequest request, SecurityUser currentUser);

    BankVerificationResultResponse checkReversePennyDropStatus(String verificationId, UUID landlordId, SecurityUser currentUser);

    BankIfscLookupResponse lookupIfsc(BankIfscLookupRequest request, SecurityUser currentUser);

    BankVerificationResultResponse verifyBankAccountSync(BankSyncVerificationRequest request, SecurityUser currentUser);

    BankVerificationResultResponse verifyUpiPennyDrop(BankUpiVerificationRequest request, SecurityUser currentUser);
}

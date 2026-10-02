package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.dto.request.LandlordPayoutRequest;
import com.bhartiyasaas.stayfile.dto.response.LandlordPayoutResponse;
import com.bhartiyasaas.stayfile.security.SecurityUser;

import java.util.List;
import java.util.UUID;

public interface LandlordPayoutService {
    LandlordPayoutResponse createPayout(LandlordPayoutRequest request, SecurityUser currentUser);
    LandlordPayoutResponse getPayoutById(UUID id, SecurityUser currentUser);
    List<LandlordPayoutResponse> getPayoutsByLandlord(UUID landlordId, SecurityUser currentUser);
    List<LandlordPayoutResponse> getPayoutsByOrganization(SecurityUser currentUser);
    LandlordPayoutResponse calculateMonthlyPayoutForLandlord(UUID landlordId, String periodMonth, SecurityUser currentUser);
    byte[] downloadPayoutPdf(UUID id, SecurityUser currentUser);
}

package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.dto.request.LandlordRequest;
import com.bhartiyasaas.stayfile.dto.response.LandlordResponse;
import com.bhartiyasaas.stayfile.security.SecurityUser;

import java.util.List;
import java.util.UUID;

public interface LandlordService {
    LandlordResponse createLandlord(LandlordRequest request, SecurityUser currentUser);
    LandlordResponse getLandlordById(UUID id, SecurityUser currentUser);
    List<LandlordResponse> getLandlordsByOrganization(SecurityUser currentUser);
    LandlordResponse updateLandlord(UUID id, LandlordRequest request, SecurityUser currentUser);
    void deleteLandlord(UUID id, SecurityUser currentUser);
}

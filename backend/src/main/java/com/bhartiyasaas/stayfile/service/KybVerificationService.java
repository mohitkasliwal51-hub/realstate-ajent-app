package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.dto.request.CinVerificationRequest;
import com.bhartiyasaas.stayfile.dto.request.GstinVerificationRequest;
import com.bhartiyasaas.stayfile.dto.request.PanToGstinRequest;
import com.bhartiyasaas.stayfile.dto.response.CinVerificationResponse;
import com.bhartiyasaas.stayfile.dto.response.GstinVerificationResponse;
import com.bhartiyasaas.stayfile.dto.response.PanToGstinResponse;
import com.bhartiyasaas.stayfile.security.SecurityUser;

public interface KybVerificationService {
    GstinVerificationResponse verifyGstin(GstinVerificationRequest request, SecurityUser currentUser);
    PanToGstinResponse findGstinsByPan(PanToGstinRequest request, SecurityUser currentUser);
    CinVerificationResponse verifyCin(CinVerificationRequest request, SecurityUser currentUser);
}

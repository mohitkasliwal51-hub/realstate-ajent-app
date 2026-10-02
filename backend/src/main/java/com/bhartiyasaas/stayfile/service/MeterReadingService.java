package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.dto.request.MeterReadingRequest;
import com.bhartiyasaas.stayfile.dto.response.MeterReadingResponse;
import com.bhartiyasaas.stayfile.security.SecurityUser;

import java.util.List;
import java.util.UUID;

public interface MeterReadingService {
    MeterReadingResponse createMeterReading(MeterReadingRequest request, SecurityUser currentUser);
    MeterReadingResponse getMeterReadingById(UUID id, SecurityUser currentUser);
    List<MeterReadingResponse> getMeterReadingsByUnit(UUID unitId, SecurityUser currentUser);
    List<MeterReadingResponse> getMeterReadingsByLease(UUID leaseId, SecurityUser currentUser);
}

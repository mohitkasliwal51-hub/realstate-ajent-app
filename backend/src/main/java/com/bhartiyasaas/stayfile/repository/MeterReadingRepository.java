package com.bhartiyasaas.stayfile.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bhartiyasaas.stayfile.entity.MeterReading;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, UUID> {
        List<MeterReading> findByUnitIdAndOrganizationId(UUID unitId, UUID organizationId);
        List<MeterReading> findByUnitIdAndOrganizationIdAndIsBilledFalseOrderByReadingDateAsc(UUID unitId, UUID organizationId);
        Optional<MeterReading> findTopByUnitIdAndOrganizationIdAndMeterTypeOrderByReadingDateDesc(
            UUID unitId, UUID organizationId, String meterType);
    Optional<MeterReading> findByIdAndOrganizationId(UUID id, UUID organizationId);
}

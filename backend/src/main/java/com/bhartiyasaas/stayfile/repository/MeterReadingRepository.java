package com.bhartiyasaas.stayfile.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bhartiyasaas.stayfile.entity.MeterReading;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, UUID> {
    List<MeterReading> findByOrganizationId(UUID organizationId);
    List<MeterReading> findByUnitId(UUID unitId);
    List<MeterReading> findByUnitIdAndIsBilledFalseOrderByReadingDateAsc(UUID unitId);
    Optional<MeterReading> findTopByUnitIdAndMeterTypeOrderByReadingDateDesc(UUID unitId, String meterType);
    Optional<MeterReading> findByIdAndOrganizationId(UUID id, UUID organizationId);
}

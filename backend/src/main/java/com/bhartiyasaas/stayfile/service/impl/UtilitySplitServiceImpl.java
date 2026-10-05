package com.bhartiyasaas.stayfile.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.dto.response.InvoiceResponse;
import com.bhartiyasaas.stayfile.entity.*;
import com.bhartiyasaas.stayfile.entity.enums.ChargeType;
import com.bhartiyasaas.stayfile.entity.enums.InvoiceStatus;
import com.bhartiyasaas.stayfile.entity.enums.InvoiceType;
import com.bhartiyasaas.stayfile.entity.enums.LeaseStatus;
import com.bhartiyasaas.stayfile.entity.enums.UserRole;
import com.bhartiyasaas.stayfile.exception.BadRequestException;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.mapper.InvoiceMapper;
import com.bhartiyasaas.stayfile.repository.*;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.UtilitySplitService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UtilitySplitServiceImpl implements UtilitySplitService {

    private final PropertyRepository propertyRepository;
    private final LeaseRepository leaseRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final InvoiceRepository invoiceRepository;
    private final OrganizationRepository organizationRepository;
    private final InvoiceMapper invoiceMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public List<InvoiceResponse> calculateAndGeneratePgUtilityInvoices(
            UUID propertyId,
            LocalDate billingMonth,
            SecurityUser currentUser) {

        UUID organizationId = currentUser.getOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));

        Property property = propertyRepository.findByIdAndOrganizationId(propertyId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + propertyId));

        LocalDate targetMonth = billingMonth != null ? billingMonth : LocalDate.now();
        LocalDate startOfMonth = targetMonth.withDayOfMonth(1);
        LocalDate endOfMonth = targetMonth.withDayOfMonth(targetMonth.lengthOfMonth());

        // 1. Retrieve all active leases in this property
        List<Lease> activeLeases = leaseRepository.findByOrganizationId(organizationId).stream()
                .filter(l -> l.getStatus() == LeaseStatus.ACTIVE)
                .filter(l -> l.getUnit() != null && l.getUnit().getProperty() != null
                        && propertyId.equals(l.getUnit().getProperty().getId()))
                .toList();

        if (activeLeases.isEmpty()) {
            log.info("No active leases found for property ID: {}. Skipping utility split.", propertyId);
            return Collections.emptyList();
        }

        int totalResidents = activeLeases.size();

        // 2. Collect unit IDs (including parent room units) for this property
        Set<UUID> allUnitIds = new HashSet<>();
        for (Lease l : activeLeases) {
            allUnitIds.add(l.getUnit().getId());
            if (l.getUnit().getParentUnit() != null) {
                allUnitIds.add(l.getUnit().getParentUnit().getId());
            }
        }

        // 3. Find unbilled meter readings for this property & its units
        List<MeterReading> propertyReadings = meterReadingRepository.findByPropertyIdAndOrganizationIdAndIsBilledFalse(propertyId, organizationId);
        List<MeterReading> unitReadings = allUnitIds.stream()
                .flatMap(uId -> meterReadingRepository.findByUnitIdAndOrganizationIdAndIsBilledFalseOrderByReadingDateAsc(uId, organizationId).stream())
                .toList();

        Map<UUID, MeterReading> readingsMap = new LinkedHashMap<>();
        for (MeterReading r : propertyReadings) {
            readingsMap.put(r.getId(), r);
        }
        for (MeterReading r : unitReadings) {
            readingsMap.put(r.getId(), r);
        }

        List<MeterReading> unbilledReadings = new ArrayList<>(readingsMap.values());
        if (unbilledReadings.isEmpty()) {
            log.info("No unbilled meter readings found for property ID: {}.", propertyId);
            return Collections.emptyList();
        }

        // 4. Classify readings
        MeterReading masterReading = null;
        MeterReading hallAcReading = null;
        Map<UUID, MeterReading> roomAcReadingsByUnit = new HashMap<>(); // Unit or ParentUnit -> Reading

        for (MeterReading r : unbilledReadings) {
            String scope = r.getMeterScope() != null ? r.getMeterScope().toUpperCase() : "ROOM_AC";
            if ("MASTER_DISCOM".equals(scope)) {
                if (masterReading == null || r.getReadingDate().isAfter(masterReading.getReadingDate())) {
                    masterReading = r;
                }
            } else if ("HALL_AC".equals(scope)) {
                if (hallAcReading == null || r.getReadingDate().isAfter(hallAcReading.getReadingDate())) {
                    hallAcReading = r;
                }
            } else {
                if (r.getUnit() != null) {
                    roomAcReadingsByUnit.put(r.getUnit().getId(), r);
                }
            }
        }

        // 5. Calculate consumption totals
        BigDecimal defaultRate = BigDecimal.valueOf(10.00);
        BigDecimal effectiveRate = defaultRate;
        if (masterReading != null && masterReading.getRatePerUnit() != null) {
            effectiveRate = masterReading.getRatePerUnit();
        }

        BigDecimal masterUnits = BigDecimal.ZERO;
        if (masterReading != null) {
            masterUnits = masterReading.getUnitsConsumed() != null ? masterReading.getUnitsConsumed() : BigDecimal.ZERO;
        }

        BigDecimal totalAcUnits = BigDecimal.ZERO;
        if (hallAcReading != null && hallAcReading.getUnitsConsumed() != null) {
            totalAcUnits = totalAcUnits.add(hallAcReading.getUnitsConsumed());
        }
        for (MeterReading r : roomAcReadingsByUnit.values()) {
            if (r.getUnitsConsumed() != null) {
                totalAcUnits = totalAcUnits.add(r.getUnitsConsumed());
            }
        }

        // If masterUnits wasn't logged, treat total units as total AC units
        if (masterUnits.compareTo(BigDecimal.ZERO) == 0 && totalAcUnits.compareTo(BigDecimal.ZERO) > 0) {
            masterUnits = totalAcUnits;
        }

        // Common units = max(0, masterUnits - totalAcUnits)
        BigDecimal commonUnits = masterUnits.subtract(totalAcUnits);
        if (commonUnits.compareTo(BigDecimal.ZERO) < 0) {
            commonUnits = BigDecimal.ZERO;
        }

        BigDecimal perPersonCommonUnits = BigDecimal.ZERO;
        BigDecimal perPersonCommonCharge = BigDecimal.ZERO;
        if (totalResidents > 0 && commonUnits.compareTo(BigDecimal.ZERO) > 0) {
            perPersonCommonUnits = commonUnits.divide(BigDecimal.valueOf(totalResidents), 2, RoundingMode.HALF_UP);
            perPersonCommonCharge = perPersonCommonUnits.multiply(effectiveRate).setScale(2, RoundingMode.HALF_UP);
        }

        // Hall AC per-person share (split across all residents who share the hall)
        BigDecimal perPersonHallUnits = BigDecimal.ZERO;
        BigDecimal perPersonHallCharge = BigDecimal.ZERO;
        if (hallAcReading != null && hallAcReading.getUnitsConsumed() != null && totalResidents > 0) {
            BigDecimal hallRate = hallAcReading.getRatePerUnit() != null ? hallAcReading.getRatePerUnit() : effectiveRate;
            perPersonHallUnits = hallAcReading.getUnitsConsumed().divide(BigDecimal.valueOf(totalResidents), 2, RoundingMode.HALF_UP);
            perPersonHallCharge = perPersonHallUnits.multiply(hallRate).setScale(2, RoundingMode.HALF_UP);
        }

        // 6. Group leases by room (Unit or ParentUnit) to count room occupants
        Map<UUID, List<Lease>> leasesByRoom = new HashMap<>();
        for (Lease lease : activeLeases) {
            UUID roomKey = lease.getUnit().getParentUnit() != null
                    ? lease.getUnit().getParentUnit().getId()
                    : lease.getUnit().getId();
            leasesByRoom.computeIfAbsent(roomKey, k -> new ArrayList<>()).add(lease);
        }

        List<Invoice> generatedInvoices = new ArrayList<>();
        long invoiceIndex = System.currentTimeMillis();

        // 7. Generate an independent UTILITY_ONLY invoice for each tenant
        for (Lease lease : activeLeases) {
            // Check if UTILITY_ONLY invoice already generated for this billing cycle
            Invoice existing = invoiceRepository.findByLeaseIdAndOrganizationIdAndBillingPeriodStartAndInvoiceType(
                    lease.getId(), organizationId, startOfMonth, InvoiceType.UTILITY_ONLY).orElse(null);
            if (existing != null) {
                generatedInvoices.add(existing);
                continue;
            }

            UUID roomKey = lease.getUnit().getParentUnit() != null
                    ? lease.getUnit().getParentUnit().getId()
                    : lease.getUnit().getId();

            List<Lease> roomOccupants = leasesByRoom.getOrDefault(roomKey, Collections.singletonList(lease));
            int roomOccupantCount = Math.max(1, roomOccupants.size());

            // Look up room AC reading (either on the room unit or child bed unit)
            MeterReading roomReading = roomAcReadingsByUnit.get(roomKey);
            if (roomReading == null) {
                roomReading = roomAcReadingsByUnit.get(lease.getUnit().getId());
            }

            BigDecimal roomAcUnits = BigDecimal.ZERO;
            BigDecimal perPersonRoomUnits = BigDecimal.ZERO;
            BigDecimal perPersonRoomCharge = BigDecimal.ZERO;
            BigDecimal roomRate = effectiveRate;
            String roomPhotoUrl = null;

            if (roomReading != null && roomReading.getUnitsConsumed() != null) {
                roomAcUnits = roomReading.getUnitsConsumed();
                roomRate = roomReading.getRatePerUnit() != null ? roomReading.getRatePerUnit() : effectiveRate;
                perPersonRoomUnits = roomAcUnits.divide(BigDecimal.valueOf(roomOccupantCount), 2, RoundingMode.HALF_UP);
                perPersonRoomCharge = perPersonRoomUnits.multiply(roomRate).setScale(2, RoundingMode.HALF_UP);
                roomPhotoUrl = roomReading.getPhotoUrl();
            }

            BigDecimal totalUtilityCharge = perPersonCommonCharge.add(perPersonRoomCharge).add(perPersonHallCharge);
            if (totalUtilityCharge.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Create UTILITY_ONLY Invoice
            Invoice invoice = new Invoice();
            invoice.setOrganization(organization);
            invoice.setLease(lease);
            invoice.setTenant(lease.getTenant());
            invoice.setUnit(lease.getUnit());
            invoice.setInvoiceNumber("UTIL-" + (invoiceIndex++));
            invoice.setInvoiceType(InvoiceType.UTILITY_ONLY);
            invoice.setStatus(InvoiceStatus.UNPAID);
            invoice.setBillingPeriodStart(startOfMonth);
            invoice.setBillingPeriodEnd(endOfMonth);
            invoice.setDueDate(LocalDate.now().plusDays(10)); // Independent due date (10 days from billing)
            invoice.setPaidAmount(BigDecimal.ZERO);
            invoice.setSubtotalAmount(totalUtilityCharge);
            invoice.setTaxAmount(BigDecimal.ZERO);
            invoice.setDiscountAmount(BigDecimal.ZERO);
            invoice.setTotalAmount(totalUtilityCharge);

            List<InvoiceLineItem> lineItems = new ArrayList<>();

            // Line 1: Common Area Electricity
            if (perPersonCommonCharge.compareTo(BigDecimal.ZERO) > 0) {
                InvoiceLineItem commonItem = new InvoiceLineItem();
                commonItem.setOrganization(organization);
                commonItem.setInvoice(invoice);
                commonItem.setChargeType(ChargeType.ELECTRICITY_BILL);
                commonItem.setDescription("Common Electricity Share (" + perPersonCommonUnits + " units @ Rs. " + effectiveRate + ")");
                commonItem.setQuantity(perPersonCommonUnits);
                commonItem.setUnitPrice(effectiveRate);
                commonItem.setAmount(perPersonCommonCharge);
                lineItems.add(commonItem);
            }

            // Line 2: Room AC Consumption
            if (perPersonRoomCharge.compareTo(BigDecimal.ZERO) > 0) {
                String roomLabel = lease.getUnit().getParentUnit() != null
                        ? "Room " + lease.getUnit().getParentUnit().getUnitNumber()
                        : "Unit " + lease.getUnit().getUnitNumber();
                InvoiceLineItem roomItem = new InvoiceLineItem();
                roomItem.setOrganization(organization);
                roomItem.setInvoice(invoice);
                roomItem.setChargeType(ChargeType.ELECTRICITY_BILL);
                roomItem.setDescription(roomLabel + " AC Share (" + perPersonRoomUnits + " units @ Rs. " + roomRate + ")");
                roomItem.setQuantity(perPersonRoomUnits);
                roomItem.setUnitPrice(roomRate);
                roomItem.setAmount(perPersonRoomCharge);
                lineItems.add(roomItem);
            }

            // Line 3: Hall AC Consumption (if applicable)
            if (perPersonHallCharge.compareTo(BigDecimal.ZERO) > 0) {
                InvoiceLineItem hallItem = new InvoiceLineItem();
                hallItem.setOrganization(organization);
                hallItem.setInvoice(invoice);
                hallItem.setChargeType(ChargeType.ELECTRICITY_BILL);
                hallItem.setDescription("Hall AC Share (" + perPersonHallUnits + " units @ Rs. " + effectiveRate + ")");
                hallItem.setQuantity(perPersonHallUnits);
                hallItem.setUnitPrice(effectiveRate);
                hallItem.setAmount(perPersonHallCharge);
                lineItems.add(hallItem);
            }

            invoice.setLineItems(lineItems);

            // 8. Build Transparent Audit Metadata
            Map<String, Object> auditMetadata = new LinkedHashMap<>();
            auditMetadata.put("utilityType", "ELECTRICITY");
            auditMetadata.put("splitFormula", "Common (Master - Total AC) / Total Residents + Room AC / Room Occupants");
            auditMetadata.put("masterDiscomUnits", masterUnits);
            auditMetadata.put("totalAcUnits", totalAcUnits);
            auditMetadata.put("commonAreaUnits", commonUnits);
            auditMetadata.put("totalPropertyResidents", totalResidents);
            auditMetadata.put("tenantCommonShareUnits", perPersonCommonUnits);
            auditMetadata.put("commonShareAmount", perPersonCommonCharge);
            auditMetadata.put("roomAcTotalUnits", roomAcUnits);
            auditMetadata.put("roomOccupantCount", roomOccupantCount);
            auditMetadata.put("tenantRoomAcUnits", perPersonRoomUnits);
            auditMetadata.put("roomAcAmount", perPersonRoomCharge);
            auditMetadata.put("ratePerUnit", effectiveRate);
            if (roomPhotoUrl != null) {
                auditMetadata.put("submeterPhotoUrl", roomPhotoUrl);
            }
            if (masterReading != null && masterReading.getPhotoUrl() != null) {
                auditMetadata.put("masterMeterPhotoUrl", masterReading.getPhotoUrl());
            }

            try {
                invoice.setMetadata(objectMapper.writeValueAsString(auditMetadata));
            } catch (Exception e) {
                log.warn("Failed to serialize audit metadata for invoice", e);
            }

            Invoice saved = invoiceRepository.save(invoice);
            generatedInvoices.add(saved);
        }

        // 9. Mark unbilled meter readings as billed
        for (MeterReading r : unbilledReadings) {
            r.setIsBilled(true);
            meterReadingRepository.save(r);
        }

        log.info("Generated {} UTILITY_ONLY invoices for property ID: {}", generatedInvoices.size(), propertyId);
        return invoiceMapper.toResponseList(generatedInvoices);
    }

    @Override
    @Scheduled(cron = "0 0 4 * * ?") // Daily at 4:00 AM
    @Transactional
    public List<InvoiceResponse> autoProcessDueUtilitySplits() {
        log.info("Running automatic scheduled utility split check...");
        List<Property> activeProperties = propertyRepository.findByIsActiveTrue();
        List<InvoiceResponse> allGenerated = new ArrayList<>();

        for (Property property : activeProperties) {
            try {
                // Check if this property has unbilled meter readings ready to split
                List<MeterReading> unbilled = meterReadingRepository.findByPropertyIdAndOrganizationIdAndIsBilledFalse(
                        property.getId(), property.getOrganization().getId());
                if (!unbilled.isEmpty()) {
                    Profile systemProfile = Profile.builder()
                            .id(UUID.randomUUID())
                            .organization(property.getOrganization())
                            .role(UserRole.ADMIN)
                            .build();
                    SecurityUser systemUser = new SecurityUser(systemProfile);

                    List<InvoiceResponse> generated = calculateAndGeneratePgUtilityInvoices(
                            property.getId(), LocalDate.now(), systemUser);
                    allGenerated.addAll(generated);
                }
            } catch (Exception e) {
                log.error("Failed auto-split for property ID: {}", property.getId(), e);
            }
        }
        return allGenerated;
    }
}

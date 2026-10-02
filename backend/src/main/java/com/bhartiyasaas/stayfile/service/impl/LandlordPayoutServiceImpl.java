package com.bhartiyasaas.stayfile.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.dto.request.LandlordPayoutRequest;
import com.bhartiyasaas.stayfile.dto.response.LandlordPayoutResponse;
import com.bhartiyasaas.stayfile.entity.*;
import com.bhartiyasaas.stayfile.entity.enums.BrokerageFeeType;
import com.bhartiyasaas.stayfile.entity.enums.MaintenanceFeeType;
import com.bhartiyasaas.stayfile.entity.enums.PayoutStatus;
import com.bhartiyasaas.stayfile.entity.enums.ReceiptType;
import com.bhartiyasaas.stayfile.exception.BadRequestException;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.mapper.LandlordPayoutMapper;
import com.bhartiyasaas.stayfile.repository.*;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.LandlordPayoutService;
import com.bhartiyasaas.stayfile.service.PdfGeneratorService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LandlordPayoutServiceImpl implements LandlordPayoutService {

    private final LandlordPayoutRepository landlordPayoutRepository;
    private final LandlordRepository landlordRepository;
    private final OrganizationRepository organizationRepository;
    private final PropertyRepository propertyRepository;
    private final ReceiptRepository receiptRepository;
    private final LeaseRepository leaseRepository;
    private final LandlordPayoutMapper landlordPayoutMapper;
    private final PdfGeneratorService pdfGeneratorService;

    @Override
    @Transactional
    public LandlordPayoutResponse createPayout(LandlordPayoutRequest request, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));

        Landlord landlord = landlordRepository.findByIdAndManagingOrganizationId(request.getLandlordId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord not found with ID: " + request.getLandlordId()));

        Property property = null;
        if (request.getPropertyId() != null) {
            property = propertyRepository.findByIdAndOrganizationId(request.getPropertyId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + request.getPropertyId()));
            if (property.getLandlord() == null || !property.getLandlord().getId().equals(landlord.getId())) {
                throw new BadRequestException("Property does not belong to the selected landlord");
            }
        }

        LandlordPayout payout = landlordPayoutMapper.toEntity(request);
        payout.setOrganization(organization);
        payout.setLandlord(landlord);
        payout.setProperty(property);
        payout.setPayoutNumber("PAY-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());

        BigDecimal gross = request.getTotalCollected() != null ? request.getTotalCollected() : BigDecimal.ZERO;
        BigDecimal comm = request.getCommissionAmount() != null ? request.getCommissionAmount() : BigDecimal.ZERO;
        BigDecimal ded = request.getDeductionsAmount() != null ? request.getDeductionsAmount() : BigDecimal.ZERO;

        if (gross.signum() < 0 || comm.signum() < 0 || ded.signum() < 0) {
            throw new BadRequestException("Payout amounts cannot be negative");
        }
        BigDecimal net = gross.subtract(comm).subtract(ded);
        payout.setTotalCollected(gross);
        payout.setCommissionAmount(comm);
        payout.setDeductionsAmount(ded);
        payout.setNetPayoutAmount(net.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : net);
        payout.setPayoutStatus(PayoutStatus.PENDING);

        LandlordPayout saved = landlordPayoutRepository.save(payout);
        return landlordPayoutMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LandlordPayoutResponse getPayoutById(UUID id, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        LandlordPayout payout = landlordPayoutRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord payout not found with ID: " + id));
        return landlordPayoutMapper.toResponse(payout);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LandlordPayoutResponse> getPayoutsByLandlord(UUID landlordId, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        landlordRepository.findByIdAndManagingOrganizationId(landlordId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord not found with ID: " + landlordId));
        return landlordPayoutMapper.toResponseList(landlordPayoutRepository.findByLandlordIdAndOrganizationId(landlordId, organizationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LandlordPayoutResponse> getPayoutsByOrganization(SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        return landlordPayoutMapper.toResponseList(landlordPayoutRepository.findByOrganizationId(organizationId));
    }

    @Override
    @Transactional
    public LandlordPayoutResponse calculateMonthlyPayoutForLandlord(UUID landlordId, String periodMonth, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));

        Landlord landlord = landlordRepository.findByIdAndManagingOrganizationId(landlordId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord not found with ID: " + landlordId));

        YearMonth period = parsePeriod(periodMonth);
        LocalDate periodStart = period.atDay(1);
        LocalDate periodEnd = period.atEndOfMonth();

        List<Lease> leases = leaseRepository.findByOrganizationId(organizationId).stream()
                .filter(l -> l.getLandlord() != null && l.getLandlord().getId().equals(landlordId))
                .filter(l -> l.getStartDate().isBefore(periodEnd.plusDays(1)) && l.getEndDate().isAfter(periodStart.minusDays(1)))
                .toList();

        BigDecimal grossRent = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal totalMaintFee = BigDecimal.ZERO;
        List<Receipt> receipts = receiptRepository.findByOrganizationId(organizationId);
        for (Lease lease : leases) {
            BigDecimal leaseGross = receipts.stream()
                    .filter(receipt -> receipt.getLease() != null && receipt.getLease().getId().equals(lease.getId()))
                    .filter(receipt -> receipt.getPaymentDate() != null
                            && !receipt.getPaymentDate().isBefore(periodStart)
                            && !receipt.getPaymentDate().isAfter(periodEnd))
                    .filter(receipt -> receipt.getReceiptType() == ReceiptType.RENT_PAYMENT)
                    .map(Receipt::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            grossRent = grossRent.add(leaseGross);

            if (lease.getBrokerageFeeType() == BrokerageFeeType.RECURRING_PERCENTAGE && lease.getBrokerageAmount() != null) {
                totalCommission = totalCommission.add(leaseGross.multiply(lease.getBrokerageAmount())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            } else if (lease.getBrokerageFeeType() == BrokerageFeeType.RECURRING_FIXED && lease.getBrokerageAmount() != null) {
                if (leaseGross.signum() > 0) {
                    totalCommission = totalCommission.add(lease.getBrokerageAmount());
                }
            } else if (lease.getBrokerageFeeType() == BrokerageFeeType.ONE_TIME
                    && lease.getStartDate().getYear() == period.getYear()
                    && lease.getStartDate().getMonth() == period.getMonth()
                    && lease.getBrokerageAmount() != null) {
                totalCommission = totalCommission.add(lease.getBrokerageAmount());
            }

            if (lease.getMaintenanceFeeType() == MaintenanceFeeType.MONTHLY && lease.getMaintenanceFeeAmount() != null) {
                totalMaintFee = totalMaintFee.add(lease.getMaintenanceFeeAmount());
            } else if (lease.getMaintenanceFeeType() == MaintenanceFeeType.ANNUAL_ONE_TIME
                    && lease.getStartDate().getYear() == period.getYear()
                    && lease.getStartDate().getMonth() == period.getMonth()
                    && lease.getMaintenanceFeeAmount() != null) {
                totalMaintFee = totalMaintFee.add(lease.getMaintenanceFeeAmount());
            }
        }

        BigDecimal netPayable = grossRent.subtract(totalCommission).subtract(totalMaintFee);

        LandlordPayout payout = new LandlordPayout();
        payout.setOrganization(organization);
        payout.setLandlord(landlord);
        payout.setPayoutNumber("PAY-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        payout.setTotalCollected(grossRent);
        payout.setCommissionAmount(totalCommission);
        payout.setDeductionsAmount(totalMaintFee);
        payout.setNetPayoutAmount(netPayable.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : netPayable);
        payout.setPayoutStatus(PayoutStatus.PENDING);

        LandlordPayout saved = landlordPayoutRepository.save(payout);
        return landlordPayoutMapper.toResponse(saved);
    }

    private YearMonth parsePeriod(String periodMonth) {
        if (periodMonth == null || periodMonth.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(periodMonth);
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("periodMonth must use YYYY-MM format");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadPayoutPdf(UUID id, SecurityUser currentUser) {
        UUID organizationId = currentUser.getOrganizationId();
        LandlordPayout payout = landlordPayoutRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord payout not found with ID: " + id));
        return pdfGeneratorService.generateLandlordPayoutPdf(payout);
    }
}

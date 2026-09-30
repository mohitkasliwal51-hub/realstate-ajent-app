package com.bhartiyasaas.stayfile.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.bhartiyasaas.stayfile.security.SecurityUser;

import com.bhartiyasaas.stayfile.dto.request.MaintenanceTicketCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.ApiResponse;
import com.bhartiyasaas.stayfile.dto.response.MaintenanceTicketResponse;
import com.bhartiyasaas.stayfile.service.MaintenanceTicketService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class MaintenanceTicketController {

    private final MaintenanceTicketService ticketService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OWNER_ADMIN', 'PROPERTY_MANAGER', 'STAFF_ASSISTANT', 'TENANT')")
    @PostMapping
    public ResponseEntity<ApiResponse<MaintenanceTicketResponse>> createTicket(
            @Valid @RequestBody MaintenanceTicketCreateRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {
        MaintenanceTicketResponse response = ticketService.createTicket(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Maintenance ticket created successfully"));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OWNER_ADMIN', 'PROPERTY_MANAGER', 'STAFF_ASSISTANT')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MaintenanceTicketResponse>>> getTicketsByOrganization(
            @AuthenticationPrincipal SecurityUser currentUser) {
        List<MaintenanceTicketResponse> response = ticketService.getTicketsByOrganization(currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OWNER_ADMIN', 'PROPERTY_MANAGER', 'STAFF_ASSISTANT', 'TENANT')")
    @GetMapping("/tenant")
    public ResponseEntity<ApiResponse<List<MaintenanceTicketResponse>>> getTicketsByTenant(
            @RequestParam UUID tenantId,
            @AuthenticationPrincipal SecurityUser currentUser) {
        List<MaintenanceTicketResponse> response = ticketService.getTicketsByTenant(tenantId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OWNER_ADMIN', 'PROPERTY_MANAGER', 'STAFF_ASSISTANT')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<MaintenanceTicketResponse>> updateTicketStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestParam String status) {
        MaintenanceTicketResponse response = ticketService.updateTicketStatus(id, currentUser, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Ticket status updated to " + status));
    }
}

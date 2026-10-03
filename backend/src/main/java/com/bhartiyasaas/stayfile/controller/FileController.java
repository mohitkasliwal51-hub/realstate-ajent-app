package com.bhartiyasaas.stayfile.controller;

import com.bhartiyasaas.stayfile.dto.response.ApiResponse;
import com.bhartiyasaas.stayfile.dto.response.FileUploadResponse;
import com.bhartiyasaas.stayfile.entity.enums.FileCategory;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.service.StorageService;
import com.bhartiyasaas.stayfile.service.storage.StorageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final StorageService storageService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROPERTY_MANAGER', 'AGENT')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") FileCategory category,
            @AuthenticationPrincipal SecurityUser currentUser) {

        FileUploadResponse response = storageService.storeFile(file, category, currentUser.getOrganizationId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "File uploaded successfully"));
    }

    @GetMapping("/private/**")
    public ResponseEntity<Resource> downloadPrivateFile(
            HttpServletRequest request,
            @AuthenticationPrincipal SecurityUser currentUser) {

        String fullPath = request.getRequestURI();
        String key = fullPath.substring(fullPath.indexOf("/private/") + 9);

        Resource resource = storageService.loadFileAsResource(key, currentUser.getOrganizationId());
        String contentType = StorageKeys.contentTypeOf(key);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}

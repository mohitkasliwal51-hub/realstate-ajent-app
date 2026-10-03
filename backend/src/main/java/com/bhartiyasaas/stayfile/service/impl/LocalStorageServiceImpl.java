package com.bhartiyasaas.stayfile.service.impl;

import com.bhartiyasaas.stayfile.dto.response.FileUploadResponse;
import com.bhartiyasaas.stayfile.entity.enums.FileCategory;
import org.springframework.security.access.AccessDeniedException;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.service.StorageService;
import com.bhartiyasaas.stayfile.service.storage.StorageKeys;
import com.bhartiyasaas.stayfile.service.storage.UploadValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageServiceImpl implements StorageService {

    private final Path rootLocation;
    private final UploadValidator uploadValidator;

    public LocalStorageServiceImpl(
            @Value("${app.storage.local-dir:./uploads}") String uploadDir,
            UploadValidator uploadValidator) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.uploadValidator = uploadValidator;
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize local storage directory", e);
        }
    }

    @Override
    public FileUploadResponse storeFile(MultipartFile file, FileCategory category, UUID organizationId) {
        UploadValidator.DetectedType detected = uploadValidator.validate(file, category);
        String key = StorageKeys.newKey(category, organizationId, detected.extension());

        Path targetPath = this.rootLocation.resolve(key).normalize();

        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String publicOrPrivatePath = category.isPublicAccess()
                    ? "/uploads/" + key
                    : "/api/v1/files/private/" + key;

            return FileUploadResponse.builder()
                    .url(publicOrPrivatePath)
                    .key(key)
                    .fileName(file.getOriginalFilename())
                    .fileType(detected.contentType())
                    .size(file.getSize())
                    .isPublic(category.isPublicAccess())
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file for category " + category, e);
        }
    }

    @Override
    public Resource loadFileAsResource(String key, UUID organizationId) {
        String validKey = StorageKeys.requireValid(key);

        if (!StorageKeys.isPublic(validKey)) {
            if (organizationId == null || !StorageKeys.belongsToOrganization(validKey, organizationId)) {
                throw new AccessDeniedException("Access denied to requested private file");
            }
        }

        try {
            Path filePath = this.rootLocation.resolve(validKey).normalize();

            if (!filePath.startsWith(this.rootLocation)) {
                throw new SecurityException("Invalid file path outside root");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found: " + key);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File not found: " + key);
        }
    }

    @Override
    public void deleteFile(String key, UUID organizationId) {
        String validKey = StorageKeys.requireValid(key);

        if (!StorageKeys.isPublic(validKey)) {
            if (organizationId == null || !StorageKeys.belongsToOrganization(validKey, organizationId)) {
                throw new AccessDeniedException("Access denied to delete private file");
            }
        }

        try {
            Path filePath = this.rootLocation.resolve(validKey).normalize();
            if (filePath.startsWith(this.rootLocation)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + key, e);
        }
    }
}

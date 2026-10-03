package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.dto.response.FileUploadResponse;
import com.bhartiyasaas.stayfile.entity.enums.FileCategory;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface StorageService {

    /**
     * Stores an uploaded file with server-determined key based on category and organization ID.
     */
    FileUploadResponse storeFile(MultipartFile file, FileCategory category, UUID organizationId);

    /**
     * Loads a stored file as a Spring Resource after verifying organization access for private files.
     */
    Resource loadFileAsResource(String key, UUID organizationId);

    /**
     * Deletes a stored file.
     */
    void deleteFile(String key, UUID organizationId);
}

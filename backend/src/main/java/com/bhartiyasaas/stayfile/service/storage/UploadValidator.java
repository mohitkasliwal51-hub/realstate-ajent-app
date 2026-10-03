package com.bhartiyasaas.stayfile.service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.bhartiyasaas.stayfile.entity.enums.FileCategory;
import com.bhartiyasaas.stayfile.exception.BadRequestException;

/**
 * Validates an upload by looking at its actual bytes. The client supplied filename and
 * Content-Type are never trusted: the stored extension and content type come from the
 * detected file signature.
 */
@Component
public class UploadValidator {

    public enum DetectedType {
        JPEG("jpg", "image/jpeg", false),
        PNG("png", "image/png", false),
        WEBP("webp", "image/webp", false),
        PDF("pdf", "application/pdf", true);

        private final String extension;
        private final String contentType;
        private final boolean pdf;

        DetectedType(String extension, String contentType, boolean pdf) {
            this.extension = extension;
            this.contentType = contentType;
            this.pdf = pdf;
        }

        public String extension() {
            return extension;
        }

        public String contentType() {
            return contentType;
        }
    }

    private final long maxBytes;

    public UploadValidator(@Value("${app.storage.max-file-size-bytes:10485760}") long maxBytes) {
        this.maxBytes = maxBytes;
    }

    public DetectedType validate(MultipartFile file, FileCategory category) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was uploaded");
        }
        if (file.getSize() > maxBytes) {
            throw new BadRequestException("File is too large. Maximum allowed size is " + (maxBytes / (1024 * 1024)) + "MB");
        }

        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        }

        DetectedType type = detect(Arrays.copyOf(head, read));
        if (type == null) {
            throw new BadRequestException("Unsupported file. Allowed: JPG, PNG, WEBP" + (category.isPdfAllowed() ? " or PDF" : ""));
        }
        if (type.pdf && !category.isPdfAllowed()) {
            throw new BadRequestException("PDF files are not allowed for " + category.name().toLowerCase().replace('_', ' '));
        }
        return type;
    }

    /** Package-private for unit tests. */
    static DetectedType detect(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return DetectedType.JPEG;
        }
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A) {
            return DetectedType.PNG;
        }
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return DetectedType.WEBP;
        }
        if (h.length >= 5 && h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F' && h[4] == '-') {
            return DetectedType.PDF;
        }
        return null;
    }
}

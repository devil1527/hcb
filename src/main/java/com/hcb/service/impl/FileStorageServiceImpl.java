package com.hcb.service.impl;

import com.hcb.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".jpg",
            ".jpeg",
            ".png",
            ".webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    private final Path uploadLocation;

    public FileStorageServiceImpl(@Value("${hcb.upload.dir:uploads}") String uploadDir) {
        this.uploadLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String storeProductImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 5MB limit.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Only JPEG, PNG, and WebP images are allowed.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Invalid file extension. Only .jpg, .jpeg, .png, and .webp are allowed.");
        }

        // Generate safe unique internal filename
        String safeFilename = UUID.randomUUID().toString() + extension;
        Path targetLocation = uploadLocation.resolve(safeFilename).normalize();

        // Prevent path traversal attack
        if (!targetLocation.startsWith(uploadLocation)) {
            throw new SecurityException("Cannot store file outside target directory.");
        }

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return safeFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store image file.", e);
        }
    }

    @Override
    public void deleteProductImage(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return;
        }
        try {
            Path filePath = uploadLocation.resolve(StringUtils.cleanPath(filename)).normalize();
            if (filePath.startsWith(uploadLocation)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            // Log and continue
        }
    }
}

package com.alumniconnect.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("png", "jpg", "jpeg", "gif", "webp", "pdf", "doc", "docx");
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS =
            Set.of("png", "jpg", "jpeg", "gif", "webp");

    private final Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();

    public FileStorageService() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Upload directory could not be created.", e);
        }
    }

    public String storeFile(MultipartFile file, String subFolder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file is not allowed");
        }
        String safeFolder = sanitizeFolder(subFolder);
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        if (originalName.contains("..")) {
            throw new IllegalArgumentException("Invalid file name");
        }
        String extension = getExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file type");
        }

        Path targetDir = uploadDir.resolve(safeFolder).normalize();
        Files.createDirectories(targetDir);
        String fileName = UUID.randomUUID() + "_" + originalName.replaceAll("\\s+", "_");
        Path targetPath = targetDir.resolve(fileName).normalize();
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    public Path loadFile(String subFolder, String fileName) {
        String safeFolder = sanitizeFolder(subFolder);
        String cleanName = StringUtils.cleanPath(fileName);
        if (cleanName.contains("..")) {
            throw new IllegalArgumentException("Invalid file name");
        }
        return uploadDir.resolve(safeFolder).resolve(cleanName).normalize();
    }

    public String storeImageFile(MultipartFile file, String subFolder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty image file is not allowed");
        }
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = getExtension(originalName).toLowerCase();
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        boolean imageContentType = contentType.startsWith("image/");
        if (!imageContentType && !ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Only image files are allowed");
        }
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported image type");
        }
        return storeFile(file, subFolder);
    }

    private String sanitizeFolder(String subFolder) {
        String normalized = StringUtils.hasText(subFolder) ? subFolder.trim() : "misc";
        if (normalized.contains("..") || normalized.contains("/") || normalized.contains("\\")) {
            throw new IllegalArgumentException("Invalid folder");
        }
        return normalized;
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1);
    }
}

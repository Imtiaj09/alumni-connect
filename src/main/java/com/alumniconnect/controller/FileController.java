package com.alumniconnect.controller;

import com.alumniconnect.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.MalformedURLException;
import java.nio.file.Path;

@Controller
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/uploads/{subFolder}/{fileName:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String subFolder, @PathVariable String fileName) {
        try {
            Path filePath = fileStorageService.loadFile(subFolder, fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            }
        } catch (MalformedURLException | IllegalArgumentException ignored) {
            // fall through
        }
        return ResponseEntity.notFound().build();
    }
}


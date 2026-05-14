package com.alumniconnect.dto;

import org.springframework.web.multipart.MultipartFile;

public class AlumniImportForm {
    private MultipartFile file;
    private Long batchId;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }
}


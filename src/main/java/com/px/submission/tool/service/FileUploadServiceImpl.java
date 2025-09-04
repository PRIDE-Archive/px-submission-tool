package com.px.submission.tool.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileUploadServiceImpl implements FileUploadService {
    
    private final Path uploadDir = Paths.get("uploads");
    
    public FileUploadServiceImpl() {
        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }
    
    @Override
    public void uploadFiles(List<MultipartFile> files) {
        for (MultipartFile file : files) {
            try {
                if (file.isEmpty()) {
                    continue;
                }
                
                Path filePath = uploadDir.resolve(file.getOriginalFilename());
                Files.copy(file.getInputStream(), filePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to store file " + file.getOriginalFilename(), e);
            }
        }
    }
} 
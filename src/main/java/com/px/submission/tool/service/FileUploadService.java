package com.px.submission.tool.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface FileUploadService {
    void uploadFiles(List<MultipartFile> files);
} 
package com.fptu.swp391.sportscentermanager.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
    String uploadClassImage(MultipartFile file);

}

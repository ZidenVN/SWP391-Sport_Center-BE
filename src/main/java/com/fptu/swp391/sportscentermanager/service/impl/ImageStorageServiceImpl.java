package com.fptu.swp391.sportscentermanager.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.fptu.swp391.sportscentermanager.enums.ErrorCode;
import com.fptu.swp391.sportscentermanager.exception.AppException;
import com.fptu.swp391.sportscentermanager.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageStorageServiceImpl implements ImageStorageService {
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "heic", "heif");

    private final Cloudinary cloudinary;

    @Override
    public String uploadClassImage(MultipartFile file) {
       if (file == null || file.isEmpty()) {
           throw new AppException(ErrorCode.IMAGE_REQUIRED);
       }

       String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
       String ext = name.contains(".") ? name.substring(name.lastIndexOf(".") + 1) : "";
       if (!ALLOWED_EXT.contains(ext)) {
           throw new AppException(ErrorCode.IMAGE_VALID_TYPE);
       }
       try {
           Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("folder", "sports-center/classes", "resource_type", "image", "format", "jpg", "transformation", new Transformation<>().width(1200).crop("limit").quality("auto")));
           return (String) result.get("secure_url");
       }catch (Exception e){
           log.error("Upload ảnh lên Cloudinary thất bại", e);
           throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
       }
    }


}

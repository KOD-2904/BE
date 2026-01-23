package com.ttthinh.shoe_shop_basic.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {
    private final Cloudinary cloudinary;

    public String upload(MultipartFile file, String folderName) {
        try{
            String publicId = UUID.randomUUID().toString(); // hoặc dùng tên file + timestamp

            Map options = ObjectUtils.asMap(// "products/images", "users/avatars"...
                    "folder", folderName,
                    "public_id", publicId,
                    "overwrite", true,                     // ghi đè nếu trùng
                    "resource_type", "image"               // mặc định là image
            );
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
            var url = (String) uploadResult.get("secure_url");  // URL an toàn dùng để lưu DB
            log.warn("upload url: {}", url);
            return url;
        }
        catch (Exception e) {
            throw new AppException(ErrorCode.UPLOAD_IMAGE_TO_CLOUD_FAILED);
        }
    }
    public String uploadImageWithTransformation(MultipartFile file) {
        try {
            Map<String, String> options = new HashMap<>();
            options.put("transformation", "c_fill,w_500,h_500");

            Map<?, ?> uploadResult = cloudinary.uploader()
                    .upload(file.getBytes(), options);

            return (String) uploadResult.get("secure_url");

        } catch (IOException e) {
            throw new RuntimeException("Upload failed");
        }
    }
}

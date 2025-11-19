package com.ptithcm.movie.external.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    /**
     * (CẬP NHẬT) Dùng cho Use Case 1: Upload file (Avatar, Poster)
     * @param file File (ảnh) người dùng upload
     * @param publicId Tên file duy nhất (vd: "streamify/movies/posters/19995")
     * @return URL của ảnh đã upload
     */
    public String uploadFile(MultipartFile file, String publicId) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                // SỬA LẠI THÀNH .getBytes()
                // (Vì poster/backdrop là file nhỏ, cách này ổn định hơn)
                file.getBytes(),
                ObjectUtils.asMap(
                        "public_id", publicId,
                        "overwrite", true,
                        "resource_type", "image" // (Chỉ định rõ là 'image')
                )
        );
        return (String) uploadResult.get("secure_url");
    }

    /**
     * Dùng cho Use Case 2: "Upload" ảnh từ TMDb
     * @param url Link ảnh TMDb (ví dụ: https://image.tmdb.org/...)
     * @param publicId Tên file duy nhất bạn muốn đặt (ví dụ: "movies/posters/19995")
     * @return URL (https) của ảnh trên Cloudinary
     */
    public String uploadFromUrl(String url, String publicId) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                url,
                ObjectUtils.asMap(
                        "public_id", publicId,
                        "overwrite", true,
                        "resource_type", "image"
                )
        );
        return (String) uploadResult.get("secure_url");
    }

    /**
     * Xóa ảnh (ví dụ: xóa avatar cũ)
     * @param publicId Tên file (publicId) của ảnh cần xóa
     */
    public void deleteImage(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}
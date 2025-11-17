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
     * Dùng cho Use Case 1: Upload avatar người dùng
     * @param file File (ảnh) người dùng upload
     * @param folder Tên thư mục trên Cloudinary (ví dụ: "avatars")
     * @return URL của ảnh đã upload
     */
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                file.getBytes(), // Upload trực tiếp mảng byte
                ObjectUtils.asMap(
                        "folder", folder // Chỉ định thư mục
                        // "public_id", "custom_name" // Tùy chọn: set tên file
                )
        );

        // Trả về "secure_url", là URL (https) của ảnh
        return (String) uploadResult.get("secure_url");
    }

    /**
     * Dùng cho Use Case 2: "Upload" ảnh từ TMDb
     * @param url Link ảnh TMDb (ví dụ: https://image.tmdb.org/...)
     * @param publicId Tên file duy nhất bạn muốn đặt (ví dụ: "movies/posters/19995")
     * @return URL (https) của ảnh trên Cloudinary
     */
    public String uploadFromUrl(String url, String publicId) throws IOException {
        // Đây là Best Practice
        // Cloudinary sẽ TỰ ĐỘNG kéo ảnh từ URL của TMDb về
        // Server của bạn không cần tải về rồi upload lên (tiết kiệm băng thông)
        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                url, // Chỉ cần truyền URL
                ObjectUtils.asMap(
                        "public_id", publicId, // Đặt tên file chính xác
                        "overwrite", true, // Ghi đè nếu file đã tồn tại
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
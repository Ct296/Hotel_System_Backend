package com.hotel.system.service;

import com.hotel.system.repository.MediaStorageDirectory;
import com.hotel.system.util.StoredMedia;
import org.springframework.web.multipart.MultipartFile;

public interface MediaStorageService {

    StoredMedia storeImage(MultipartFile file, MediaStorageDirectory directory, String entityKey);

    void deleteByPublicPath(String publicPath);
}
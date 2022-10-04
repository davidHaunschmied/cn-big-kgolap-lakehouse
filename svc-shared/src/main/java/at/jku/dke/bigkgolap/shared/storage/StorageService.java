package at.jku.dke.bigkgolap.shared.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface StorageService {

    void store(InputStream inputStream, String storedName);

    void store(MultipartFile file, String storedName);

    TieredStorageInputStream getInputStream(String storedName);

    boolean exists(String storedName);

    void triggerTieredStorageCacheRetention();

    /**
     * @throws RuntimeException if clearing fails
     */
    void clearAll();
}

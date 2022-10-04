package at.jku.dke.bigkgolap.shared.storage;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Objects;

@Slf4j
public class LocalStorageService implements StorageService {

    private final String dir;

    public LocalStorageService(String dir) {
        this.dir = Objects.requireNonNull(dir);
        validateStorageDir();
    }

    @Override
    public void store(InputStream inputStream, String storedName) {
        File file = toFile(storedName);
        try (OutputStream outputStream = new FileOutputStream(file)) {
            IOUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void store(MultipartFile file, String storedName) {
        try {
            file.transferTo(toFile(storedName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TieredStorageInputStream getInputStream(String storedName) {
        try {
            File file = toFile(storedName);
            if (!file.setLastModified(System.currentTimeMillis())) {
                log.error("Could not set last modified date on file!");
            }
            return new TieredStorageInputStream(new FileInputStream(file), true);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean exists(String storedName) {
        return toFile(storedName).exists();
    }

    @Override
    public void triggerTieredStorageCacheRetention() {
        log.info("File retention triggered. Nothing to do since there is no cache used!");
    }

    public File toFile(String storedName) {
        return new File(dir + "/" + storedName);
    }

    public File getDir() {
        return new File(dir);
    }

    @Override
    public void clearAll() {
        final File storageDir = getDir();
        File[] files = storageDir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (!file.delete()) {
                    throw new RuntimeException("Could not clean storage directory because the following file could not be deleted: " + file.getAbsolutePath());
                }
            }
        }
    }

    private void validateStorageDir() {
        final File storageDir = getDir();
        if (!storageDir.isDirectory() && !storageDir.mkdirs()) {
            throw new IllegalArgumentException(storageDir.getAbsolutePath() + " is not a directory!");
        }
    }
}

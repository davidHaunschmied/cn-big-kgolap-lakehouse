package at.jku.dke.bigkgolap.bgprocessor.retention;

import at.jku.dke.bigkgolap.shared.config.LakehouseConfig;
import at.jku.dke.bigkgolap.shared.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class LocalStorageFileRetention {

    @Autowired
    private LakehouseConfig config;

    @Autowired
    private StorageService storageService;

    @Scheduled(fixedDelay = 60 * 60 * 1000) // 1 hour
    public void retainLocalFiles() {
        storageService.triggerTieredStorageCacheRetention();
    }
}

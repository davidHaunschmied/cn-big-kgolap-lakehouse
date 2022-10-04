package at.jku.dke.bigkgolap.shared.config;

import at.jku.dke.bigkgolap.shared.LakehouseInitializer;
import at.jku.dke.bigkgolap.shared.storage.LocalStorageService;
import at.jku.dke.bigkgolap.shared.storage.S3StorageService;
import at.jku.dke.bigkgolap.shared.storage.StorageService;
import com.datastax.oss.driver.api.core.CqlSession;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.data.cassandra.core.cql.AsyncCqlTemplate;

@Getter
@Setter
@Configuration
@ComponentScan("at.jku.dke.bigkgolap.shared")
@ConfigurationProperties(prefix = "lakehouse")
@PropertySource("classpath:application-base.properties")
@Slf4j
public class LakehouseConfig {

    private String username;
    private String password;

    // Resetting options
    private boolean reset = false;
    private boolean clearStorageDirOnReset = true;

    // Cassandra
    private String cassandraAwsRegion;
    private String cassSetupFile;
    private String cassCleanFile;

    // Storage
    private boolean useS3Storage;

    // S3
    private String s3Region;
    private String s3Bucket;
    private String localCacheDir;
    private int localFileCacheTimeoutHours;

    // Local
    private String storageDir;

    // Surface & Bed
    private String bedHost;
    private int bedGrpcPort;
    private String surfaceHost;
    private int surfaceHttpPort;

    // Redis
    private boolean useRedisCache;
    private String redisHost;
    private int redisPort;
    private int graphCacheTimeoutMinutes;

    // SQS
    private String fileIngestionSqsQueueRegion;
    private String fileIngestionSqsQueue;

    // Graph
    private String graphBaseUri;

    // For testing purposes only
    private boolean testContext = false;

    @Bean
    public AsyncCqlTemplate asyncCqlTemplate(CqlSession session, StorageService storageService) {
        AsyncCqlTemplate cassie = new AsyncCqlTemplate(session);
        new LakehouseInitializer(cassie, this, storageService).init();
        return cassie;
    }

    @Bean
    @Scope("singleton")
    public StorageService storageService() {
        if (useS3Storage) {
            log.info("Using S3 storage layer. Config: [region = {}, bucket = {}, " +
                            "localCacheDir = {}, cacheTimeoutHours = {}]",
                    s3Region, s3Bucket, storageDir, localFileCacheTimeoutHours);
            return new S3StorageService(this, new LocalStorageService(localCacheDir));
        }
        log.info("Using local storage layer. Config: [directory = {}]", storageDir);
        return new LocalStorageService(storageDir);
    }
}

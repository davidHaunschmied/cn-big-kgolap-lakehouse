package at.jku.dke.bigkgolap.shared.storage;

import at.jku.dke.bigkgolap.shared.config.LakehouseConfig;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Iterator;

@Slf4j
public class S3StorageService implements StorageService {

    private final AmazonS3 s3;
    private final TransferManager transferManager;
    private final LakehouseConfig config;
    private final LocalStorageService localCache;

    public S3StorageService(LakehouseConfig config, LocalStorageService localCache) {
        this.s3 = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.fromName(config.getS3Region()))
                .build();
        this.transferManager = TransferManagerBuilder.standard().withS3Client(s3).build();
        this.config = config;
        this.localCache = localCache;
    }

    @Override
    public void store(InputStream inputStream, String storedName) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public void store(MultipartFile file, String storedName) {
        try (InputStream is = file.getInputStream()) {
            var metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            this.s3.putObject(config.getS3Bucket(), storedName, is, metadata);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TieredStorageInputStream getInputStream(String storedName) {
      //  if (localCache.exists(storedName)) {
      //      // file is available locally
      //      return new TieredStorageInputStream(localCache.getInputStream(storedName), true);
      //  }
      //  cacheFileLocallyAsync(storedName);
        return new TieredStorageInputStream(this.s3.getObject(config.getS3Bucket(), storedName).getObjectContent(), false);
    }

    @Override
    public boolean exists(String storedName) {
        return s3.doesObjectExist(config.getS3Bucket(), storedName);
    }

    @Override
    public void triggerTieredStorageCacheRetention() {
        long retentionTime = System.currentTimeMillis() - localFileCacheTimeoutMillis();
        log.info("Tiered storage cache retention triggered. Removing files from local disk with a last modified timestamp before {}",
                LocalDateTime.ofInstant(Instant.ofEpochMilli(retentionTime), ZoneId.systemDefault()));
        deleteFilesOlderThan(retentionTime);
    }


    private void deleteFilesOlderThan(long timestamp) {
        for (File toDelete : localCache.getDir().listFiles(f -> f.lastModified() < timestamp)) {
            if (toDelete.delete()) {
                log.info("Delete cached file {}", toDelete.getAbsoluteFile());
            } else {
                log.error("Could not delete cached file {}", toDelete.getAbsoluteFile());
            }
        }
    }

    private long localFileCacheTimeoutMillis() {
        return config.getLocalFileCacheTimeoutHours() * 60 * 60 * 1000;
    }

    @Async
    protected void cacheFileLocallyAsync(String storedName) {
        File file = localCache.toFile(storedName);

        if (file.exists()) {
            return;
        }

        try (RandomAccessFile reader = new RandomAccessFile(file, "rw");
             FileLock lock = reader.getChannel().lock()) {
            reader.write(this.s3.getObject(config.getS3Bucket(), storedName).getObjectContent().readAllBytes());
        } catch (IOException e) {
            log.error("Could not download file with name {} to destination {}!", storedName, file.getAbsolutePath(), e);
        } catch (OverlappingFileLockException ignore) {
            // file is locked
        }
    }

    @Override
    public void clearAll() {
        // from https://docs.aws.amazon.com/AmazonS3/latest/userguide/delete-bucket.html

        ObjectListing objectListing = s3.listObjects(config.getS3Bucket());
        while (true) {
            Iterator<S3ObjectSummary> objIter = objectListing.getObjectSummaries().iterator();
            while (objIter.hasNext()) {
                s3.deleteObject(config.getS3Bucket(), objIter.next().getKey());
            }

            // If the bucket contains many objects, the listObjects() call
            // might not return all of the objects in the first listing. Check to
            // see whether the listing was truncated. If so, retrieve the next page of objects
            // and delete them.
            if (objectListing.isTruncated()) {
                objectListing = s3.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }

        // Delete all object versions (required for versioned buckets).
        VersionListing versionList = s3.listVersions(new ListVersionsRequest().withBucketName(config.getS3Bucket()));
        while (true) {
            Iterator<S3VersionSummary> versionIter = versionList.getVersionSummaries().iterator();
            while (versionIter.hasNext()) {
                S3VersionSummary vs = versionIter.next();
                s3.deleteVersion(config.getS3Bucket(), vs.getKey(), vs.getVersionId());
            }

            if (versionList.isTruncated()) {
                versionList = s3.listNextBatchOfVersions(versionList);
            } else {
                break;
            }
        }
    }
}

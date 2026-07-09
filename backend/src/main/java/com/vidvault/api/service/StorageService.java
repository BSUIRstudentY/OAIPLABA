package com.vidvault.api.service;

import com.vidvault.api.config.StorageProperties;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final MinioClient client;
    private final MinioClient presignClient;
    private final StorageProperties props;

    public StorageService(MinioClient minioClient,
                          @org.springframework.beans.factory.annotation.Qualifier("minioPresignClient")
                          MinioClient presignClient,
                          StorageProperties props) {
        this.client = minioClient;
        this.presignClient = presignClient;
        this.props = props;
    }

    @PostConstruct
    public void init() {
        // Storage may not be reachable yet at startup; retry a few times.
        for (int attempt = 1; attempt <= 10; attempt++) {
            if (ensureBucket()) {
                return;
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.warn("Storage bucket '{}' could not be ensured at startup; will retry on first upload",
                props.getBucket());
    }

    private boolean ensureBucket() {
        try {
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(props.getBucket()).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(props.getBucket()).build());
                log.info("Created storage bucket '{}'", props.getBucket());
            }
            return true;
        } catch (Exception e) {
            log.debug("Bucket check failed: {}", e.getMessage());
            return false;
        }
    }

    public void upload(String objectKey, MultipartFile file) {
        ensureBucket();
        try (InputStream in = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType() != null ? file.getContentType() : "video/mp4")
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to store video file: " + e.getMessage(), e);
        }
    }

    /**
     * Presigned, time-limited URL for downloading/streaming the object. Signed
     * against the public endpoint so the host matches what the browser requests.
     */
    public String presignedGetUrl(String objectKey) {
        try {
            return presignClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .expiry(1, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create download URL: " + e.getMessage(), e);
        }
    }

    public void deleteQuietly(String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to delete object {}: {}", objectKey, e.getMessage());
        }
    }

    public Map<String, String> info() {
        return Map.of("bucket", props.getBucket(), "endpoint", props.getEndpoint());
    }
}

package com.vidvault.api.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class MinioConfig {

    /**
     * Client used for server-side operations (upload, bucket management). Points
     * at the internally reachable endpoint (e.g. {@code http://minio:9000}).
     */
    @Bean
    public MinioClient minioClient(StorageProperties props) {
        return MinioClient.builder()
                .endpoint(props.getEndpoint())
                .region("us-east-1")
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
    }

    /**
     * Client used <em>only</em> to generate presigned URLs. It points at the
     * browser-reachable public endpoint (e.g. {@code http://localhost:9000}).
     * Presigning is an offline HMAC operation, so this client never needs to
     * connect; but the URL host must match what the browser will request, or the
     * SigV4 signature will not verify.
     */
    @Bean
    public MinioClient minioPresignClient(StorageProperties props) {
        String endpoint = (props.getPublicEndpoint() != null && !props.getPublicEndpoint().isBlank())
                ? props.getPublicEndpoint()
                : props.getEndpoint();
        return MinioClient.builder()
                .endpoint(endpoint)
                .region("us-east-1")
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
    }
}

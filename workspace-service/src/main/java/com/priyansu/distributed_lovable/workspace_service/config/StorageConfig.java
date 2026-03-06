package com.priyansu.distributed_lovable.workspace_service.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "minio")
@Data
public class StorageConfig {  //minio client config


    private String url;  //no need to use @Value if used  @ConfigurationProperties
    private String accessKey;
    private String secretKey;

    @Bean
    public MinioClient minioClient() {  //created minio client
        return  MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }
}

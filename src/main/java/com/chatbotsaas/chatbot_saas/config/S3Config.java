package com.chatbotsaas.chatbot_saas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Configuración del cliente S3 apuntado a Cloudflare R2.
 *
 * R2 es S3-compatible: usamos el AWS SDK estándar pero con un endpoint
 * custom (https://&lt;account_id&gt;.r2.cloudflarestorage.com). La región es
 * "auto" por convención de R2 (no usa regiones tradicionales como AWS).
 *
 * El bucket está scoped por API token a un único bucket — el cliente
 * NO puede acceder a otros buckets de la cuenta aunque sepa sus nombres.
 *
 * Path-style access habilitado (forzado por R2): las URLs son
 * https://endpoint/bucket/key en lugar de https://bucket.endpoint/key.
 */
@Configuration
public class S3Config {

    @Value("${app.storage.endpoint}")
    private String endpoint;

    @Value("${app.storage.region}")
    private String region;

    @Value("${app.storage.access-key-id}")
    private String accessKeyId;

    @Value("${app.storage.secret-access-key}")
    private String secretAccessKey;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                accessKeyId, secretAccessKey
        );

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build()
                )
                .build();
    }
}

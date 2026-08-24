package com.example.bai2.config;

import com.langfuse.client.LangfuseClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LangfuseProperties.class)
public class LangfuseConfig {
    private final LangfuseProperties langfuseProperties;

    public LangfuseConfig(LangfuseProperties langfuseProperties) {
        this.langfuseProperties = langfuseProperties;
    }

    @Bean
    public LangfuseClient langfuseClient() {
        return LangfuseClient.builder()
                .url(langfuseProperties.getBaseUrl())
                .credentials(
                        langfuseProperties.getPublicKey(),
                        langfuseProperties.getSecretKey()
                )
                .build();
    }
}

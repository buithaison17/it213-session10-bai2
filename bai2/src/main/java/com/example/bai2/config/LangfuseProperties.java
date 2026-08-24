package com.example.bai2.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "langfuse")
@Data
public class LangfuseProperties {
    private String publicKey;
    private String secretKey;
    private String baseUrl = "https://cloud.langfuse.com";
    private boolean enabled = true;
}

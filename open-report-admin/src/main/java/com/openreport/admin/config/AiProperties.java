package com.openreport.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "open-report.ai")
public class AiProperties {

    private Boolean enabled = false;

    private String provider = "openai";

    private String apiKey;

    private String apiUrl = "https://api.openai.com/v1/chat/completions";

    private String model = "gpt-3.5-turbo";

    private Integer timeout = 60;

    private Integer maxTokens = 2048;

    private Double temperature = 0.7;
}

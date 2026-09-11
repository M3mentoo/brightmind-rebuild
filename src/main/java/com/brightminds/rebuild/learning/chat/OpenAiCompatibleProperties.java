package com.brightminds.rebuild.learning.chat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.tutor.openai-compatible")
public class OpenAiCompatibleProperties {

    private String baseUrl = "";
    private String apiKey = "";
    private String model = "qwen-plus";

    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(5);

    @Min(0)
    @Max(3)
    private int maxRetries = 1;

    @NotNull
    private Duration retryBackoff = Duration.ofMillis(200);

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Duration getRetryBackoff() {
        return retryBackoff;
    }

    public void setRetryBackoff(Duration retryBackoff) {
        this.retryBackoff = retryBackoff;
    }

    @AssertTrue(message = "app.tutor.openai-compatible.connect-timeout 必须大于0")
    public boolean isConnectTimeoutPositive() {
        return connectTimeout != null && !connectTimeout.isZero() && !connectTimeout.isNegative();
    }

    @AssertTrue(message = "app.tutor.openai-compatible.retry-backoff 必须大于0")
    public boolean isRetryBackoffPositive() {
        return retryBackoff != null && !retryBackoff.isZero() && !retryBackoff.isNegative();
    }
}

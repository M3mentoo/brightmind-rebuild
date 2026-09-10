package com.brightminds.rebuild.learning.chat;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.tutor")
public class TutorProperties {

    private String provider = "simulated";

    @NotNull
    private Duration streamIdleTimeout = Duration.ofSeconds(8);

    @NotNull
    private Duration simulatedTokenDelay = Duration.ofMillis(35);

    @Min(2)
    private int historyMaxMessages = 20;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Duration getStreamIdleTimeout() {
        return streamIdleTimeout;
    }

    public void setStreamIdleTimeout(Duration streamIdleTimeout) {
        this.streamIdleTimeout = streamIdleTimeout;
    }

    public Duration getSimulatedTokenDelay() {
        return simulatedTokenDelay;
    }

    public void setSimulatedTokenDelay(Duration simulatedTokenDelay) {
        this.simulatedTokenDelay = simulatedTokenDelay;
    }

    public int getHistoryMaxMessages() {
        return historyMaxMessages;
    }

    public void setHistoryMaxMessages(int historyMaxMessages) {
        this.historyMaxMessages = historyMaxMessages;
    }

    @AssertTrue(message = "app.tutor.history-max-messages 必须是偶数")
    public boolean isHistoryMaxMessagesEven() {
        return historyMaxMessages % 2 == 0;
    }
}

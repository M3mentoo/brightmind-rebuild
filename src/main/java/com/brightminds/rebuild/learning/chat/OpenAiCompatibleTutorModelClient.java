package com.brightminds.rebuild.learning.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

@Component
@ConditionalOnProperty(name = "app.tutor.provider", havingValue = "openai-compatible")
public class OpenAiCompatibleTutorModelClient implements TutorModelClient {

    private static final Logger LOGGER = Logger.getLogger(OpenAiCompatibleTutorModelClient.class.getName());
    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_STRING_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final OpenAiCompatibleProperties properties;

    public OpenAiCompatibleTutorModelClient(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            OpenAiCompatibleProperties properties) {
        validate(properties);
        HttpClient httpClient = HttpClient.create()
                .option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        Math.toIntExact(properties.getConnectTimeout().toMillis()));
        this.webClient = webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(withTrailingSlash(properties.getBaseUrl().trim()))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey().trim())
                .build();
        this.objectMapper = objectMapper;
        this.model = properties.getModel().trim();
        this.properties = properties;
    }

    @Override
    public Flux<String> stream(TutorPrompt prompt) {
        ChatCompletionRequest request = new ChatCompletionRequest(
                model, toMessages(prompt), true);

        return Flux.defer(() -> executeObserved(request));
    }

    private Flux<String> executeObserved(ChatCompletionRequest request) {
        String requestId = UUID.randomUUID().toString();
        long startedAtNanos = System.nanoTime();
        AtomicInteger attempts = new AtomicInteger();
        AtomicBoolean emittedAnyDelta = new AtomicBoolean();

        Retry retry = Retry.backoff(properties.getMaxRetries(), properties.getRetryBackoff())
                .jitter(0)
                .filter(error -> !emittedAnyDelta.get() && isRetryable(error))
                .doBeforeRetry(signal -> LOGGER.info(() -> "model_retry requestId=%s nextAttempt=%d reason=%s"
                        .formatted(requestId, signal.totalRetries() + 2, errorCategory(signal.failure()))))
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());

        return executeRequest(request)
                .doOnSubscribe(subscription -> attempts.incrementAndGet())
                .doOnNext(delta -> emittedAnyDelta.set(true))
                .retryWhen(retry)
                .doFinally(signal -> {
                    long elapsedMillis = (System.nanoTime() - startedAtNanos) / 1_000_000;
                    LOGGER.info(() -> "model_call requestId=%s provider=openai-compatible model=%s attempts=%d result=%s elapsedMs=%d"
                            .formatted(requestId, model, attempts.get(), signal.name(), elapsedMillis));
                });
    }

    private Flux<String> executeRequest(ChatCompletionRequest request) {
        return webClient.post()
                .uri("chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .then(Mono.error(new UpstreamModelException(
                                        response.statusCode().value(),
                                        isRetryableStatus(response.statusCode().value())))))
                .bodyToFlux(SSE_STRING_TYPE)
                .map(ServerSentEvent::data)
                .filter(data -> data != null && !data.isBlank())
                .takeUntil("[DONE]"::equals)
                .<String>handle((data, sink) -> {
                    if ("[DONE]".equals(data)) {
                        return;
                    }
                    String delta = extractContent(data);
                    if (delta != null && !delta.isEmpty()) {
                        sink.next(delta);
                    }
                });
    }

    private boolean isRetryable(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof UpstreamModelException upstreamException) {
                return upstreamException.retryable();
            }
            if (current instanceof WebClientRequestException || current instanceof IOException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private String errorCategory(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof UpstreamModelException upstreamException) {
                return upstreamException.statusCode() == null
                        ? "provider_protocol"
                        : "upstream_http_" + upstreamException.statusCode();
            }
            if (current instanceof WebClientRequestException || current instanceof IOException) {
                return "network";
            }
            current = current.getCause();
        }
        return "non_retryable";
    }

    private List<ChatCompletionMessage> toMessages(TutorPrompt prompt) {
        List<ChatCompletionMessage> messages = new ArrayList<>();
        messages.add(new ChatCompletionMessage("system", prompt.systemInstruction()));
        prompt.messages().stream()
                .map(message -> new ChatCompletionMessage(
                        message.role().name().toLowerCase(Locale.ROOT), message.content()))
                .forEach(messages::add);
        return List.copyOf(messages);
    }

    private String extractContent(String data) {
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return null;
            }
            JsonNode content = choices.get(0).path("delta").path("content");
            return content.isTextual() ? content.textValue() : null;
        } catch (JsonProcessingException exception) {
            throw new UpstreamModelException("Model provider returned malformed stream data", exception);
        }
    }

    private void validate(OpenAiCompatibleProperties properties) {
        requireConfigured("TUTOR_MODEL_BASE_URL", properties.getBaseUrl());
        requireConfigured("DASHSCOPE_API_KEY", properties.getApiKey());
        requireConfigured("TUTOR_MODEL_NAME", properties.getModel());
    }

    private void requireConfigured(String environmentVariable, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(environmentVariable + " must be configured for openai-compatible provider");
        }
    }

    private String withTrailingSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    private record ChatCompletionRequest(
            String model,
            List<ChatCompletionMessage> messages,
            boolean stream) {
    }

    private record ChatCompletionMessage(String role, String content) {
    }
}

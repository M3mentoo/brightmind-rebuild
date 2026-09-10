package com.brightminds.rebuild.learning.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(name = "app.tutor.provider", havingValue = "openai-compatible")
public class OpenAiCompatibleTutorModelClient implements TutorModelClient {

    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_STRING_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiCompatibleTutorModelClient(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            OpenAiCompatibleProperties properties) {
        validate(properties);
        this.webClient = webClientBuilder
                .baseUrl(withTrailingSlash(properties.getBaseUrl().trim()))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey().trim())
                .build();
        this.objectMapper = objectMapper;
        this.model = properties.getModel().trim();
    }

    @Override
    public Flux<String> stream(TutorPrompt prompt) {
        ChatCompletionRequest request = new ChatCompletionRequest(
                model, toMessages(prompt), true);

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
                                        "Model provider returned HTTP " + response.statusCode().value()))))
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

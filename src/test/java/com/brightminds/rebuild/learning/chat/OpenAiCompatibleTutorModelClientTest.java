package com.brightminds.rebuild.learning.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.brightminds.rebuild.learning.session.LearningSessionResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

class OpenAiCompatibleTutorModelClientTest {

    private static final String FAKE_API_KEY = "fake-test-key";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DisposableServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.disposeNow();
        }
    }

    @Test
    void shouldSendCompatibleRequestAndParseStreamingDeltas() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        server = HttpServer.create()
                .port(0)
                .route(routes -> routes.post("/v1/chat/completions", (request, response) -> {
                    authorization.set(request.requestHeaders().get(HttpHeaders.AUTHORIZATION));
                    return request.receive().aggregate().asString()
                            .flatMap(body -> {
                                requestBody.set(body);
                                response.status(HttpStatus.OK.value());
                                response.header(HttpHeaderNames.CONTENT_TYPE.toString(), MediaType.TEXT_EVENT_STREAM_VALUE);
                                return response.sendString(Flux.just(
                                                "data:{\"choices\":[{\"delta\":{\"content\":\"你\"}}]}\n\n",
                                                "data:{\"choices\":[{\"delta\":{\"content\":\"好\"}}]}\n\n",
                                                "data:[DONE]\n\n"))
                                        .then();
                            });
                }))
                .bindNow();

        OpenAiCompatibleTutorModelClient client = clientFor(server.port());
        List<String> deltas = client.stream(prompt()).collectList().block(Duration.ofSeconds(2));

        assertThat(deltas).containsExactly("你", "好");
        assertThat(authorization.get()).isEqualTo("Bearer " + FAKE_API_KEY);
        JsonNode requestJson = objectMapper.readTree(requestBody.get());
        assertThat(requestJson.path("model").asText()).isEqualTo("qwen-plus");
        assertThat(requestJson.path("stream").asBoolean()).isTrue();
        assertThat(requestJson.path("messages").get(0).path("role").asText()).isEqualTo("system");
        assertThat(requestJson.path("messages").get(1).path("role").asText()).isEqualTo("user");
    }

    @Test
    void shouldHideProviderBodyAndApiKeyWhenUpstreamRejectsRequest() {
        server = HttpServer.create()
                .port(0)
                .route(routes -> routes.post("/v1/chat/completions", (request, response) -> response
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .sendString(Flux.just("provider detail containing sensitive context"))))
                .bindNow();

        OpenAiCompatibleTutorModelClient client = clientFor(server.port());

        assertThatThrownBy(() -> client.stream(prompt()).collectList().block(Duration.ofSeconds(2)))
                .isInstanceOf(UpstreamModelException.class)
                .hasMessageContaining("HTTP 401")
                .hasMessageNotContaining(FAKE_API_KEY)
                .hasMessageNotContaining("sensitive context");
    }

    @Test
    void shouldFailFastWhenApiKeyIsMissing() {
        OpenAiCompatibleProperties properties = properties(8080);
        properties.setApiKey(" ");

        assertThatThrownBy(() -> new OpenAiCompatibleTutorModelClient(
                WebClient.builder(), objectMapper, properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DASHSCOPE_API_KEY");
    }

    private OpenAiCompatibleTutorModelClient clientFor(int port) {
        return new OpenAiCompatibleTutorModelClient(
                WebClient.builder(), objectMapper, properties(port));
    }

    private OpenAiCompatibleProperties properties(int port) {
        OpenAiCompatibleProperties properties = new OpenAiCompatibleProperties();
        properties.setBaseUrl("http://127.0.0.1:" + port + "/v1");
        properties.setApiKey(FAKE_API_KEY);
        properties.setModel("qwen-plus");
        return properties;
    }

    private TutorPrompt prompt() {
        LearningSessionResponse session = new LearningSessionResponse(
                "session-1", "小明", 6, "恐龙", "ACTIVE", Instant.parse("2026-09-10T08:00:00Z"));
        return new TutorPrompt(
                session,
                "你是儿童启蒙导师",
                List.of(new ConversationMessage(
                        ConversationRole.USER, "恐龙是什么？", Instant.parse("2026-09-10T08:01:00Z"))));
    }
}

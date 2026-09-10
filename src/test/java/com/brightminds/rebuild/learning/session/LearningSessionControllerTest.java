package com.brightminds.rebuild.learning.session;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.brightminds.rebuild.common.exception.BusinessException;
import com.brightminds.rebuild.common.exception.ErrorCode;
import com.brightminds.rebuild.common.exception.GlobalExceptionHandler;
import com.brightminds.rebuild.learning.chat.ChatStreamPayload;
import com.brightminds.rebuild.learning.chat.TutorChatService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

@WebFluxTest(LearningSessionController.class)
@Import(GlobalExceptionHandler.class)
class LearningSessionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private LearningSessionService learningSessionService;

    @MockitoBean
    private TutorChatService tutorChatService;

    @Test
    void shouldCreateLearningSession() {
        LearningSessionResponse response = new LearningSessionResponse(
                "session-1", "小明", 6, "恐龙", "ACTIVE", Instant.parse("2026-09-10T08:00:00Z"));
        when(learningSessionService.create(any(CreateLearningSessionRequest.class))).thenReturn(response);

        webTestClient.post()
                .uri("/api/learning-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"childName":"小明","childAge":6,"topic":"恐龙"}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.code").isEqualTo(201)
                .jsonPath("$.message").isEqualTo("created")
                .jsonPath("$.data.sessionId").isEqualTo("session-1")
                .jsonPath("$.data.status").isEqualTo("ACTIVE");
    }

    @Test
    void shouldReturnValidationDetailsWhenRequestIsInvalid() {
        webTestClient.post()
                .uri("/api/learning-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"childName":" ","childAge":2,"topic":""}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(40001)
                .jsonPath("$.message").isEqualTo("请求参数不合法")
                .jsonPath("$.data.violations.length()").isEqualTo(3);

        verifyNoInteractions(learningSessionService);
    }

    @Test
    void shouldReturnConflictWhenSessionAlreadyExists() {
        when(learningSessionService.create(any(CreateLearningSessionRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.SESSION_ALREADY_EXISTS));

        webTestClient.post()
                .uri("/api/learning-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"childName":"小明","childAge":6,"topic":"恐龙"}
                        """)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo(40901)
                .jsonPath("$.message").isEqualTo("相同儿童和主题的学习会话已存在")
                .jsonPath("$.data").doesNotExist();
    }

    @Test
    void shouldStreamTutorEventsForExistingSession() {
        LearningSessionResponse session = new LearningSessionResponse(
                "session-1", "小明", 6, "恐龙", "ACTIVE", Instant.parse("2026-09-10T08:00:00Z"));
        ChatStreamPayload payload = new ChatStreamPayload("session-1", "message-1", "先观察", null);
        when(learningSessionService.getRequired("session-1")).thenReturn(session);
        when(tutorChatService.streamReply(eq(session), eq("霸王龙有什么特点？")))
                .thenReturn(Flux.just(ServerSentEvent.builder(payload)
                        .id("message-1")
                        .event("delta")
                        .build()));

        webTestClient.post()
                .uri("/api/learning-sessions/session-1/messages:stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue("""
                        {"message":"霸王龙有什么特点？"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBody(String.class)
                .value(body -> org.assertj.core.api.Assertions.assertThat(body)
                        .contains("event:delta", "先观察"));
    }

    @Test
    void shouldReturnNotFoundBeforeStartingStream() {
        when(learningSessionService.getRequired("missing"))
                .thenThrow(new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        webTestClient.post()
                .uri("/api/learning-sessions/missing/messages:stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue("""
                        {"message":"你好"}
                        """)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo(40401)
                .jsonPath("$.message").isEqualTo("学习会话不存在");
    }
}

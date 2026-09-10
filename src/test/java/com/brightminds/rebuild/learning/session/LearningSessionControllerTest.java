package com.brightminds.rebuild.learning.session;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.brightminds.rebuild.common.exception.BusinessException;
import com.brightminds.rebuild.common.exception.ErrorCode;
import com.brightminds.rebuild.common.exception.GlobalExceptionHandler;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(LearningSessionController.class)
@Import(GlobalExceptionHandler.class)
class LearningSessionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private LearningSessionService learningSessionService;

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
}

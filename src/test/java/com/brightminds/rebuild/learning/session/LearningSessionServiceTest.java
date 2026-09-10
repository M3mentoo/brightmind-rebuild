package com.brightminds.rebuild.learning.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brightminds.rebuild.common.exception.BusinessException;
import com.brightminds.rebuild.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class LearningSessionServiceTest {

    private final LearningSessionService learningSessionService = new LearningSessionService();

    @Test
    void shouldNormalizeInputAndCreateActiveSession() {
        LearningSessionResponse response = learningSessionService.create(
                new CreateLearningSessionRequest(" 小明 ", 6, " 恐龙 "));

        assertThat(response.sessionId()).isNotBlank();
        assertThat(response.childName()).isEqualTo("小明");
        assertThat(response.topic()).isEqualTo("恐龙");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void shouldRejectDuplicateActiveSessionAtomically() {
        CreateLearningSessionRequest request = new CreateLearningSessionRequest("小明", 6, "恐龙");
        learningSessionService.create(request);

        assertThatThrownBy(() -> learningSessionService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.SESSION_ALREADY_EXISTS);
    }

    @Test
    void shouldFindCreatedSessionById() {
        LearningSessionResponse created = learningSessionService.create(
                new CreateLearningSessionRequest("小明", 6, "恐龙"));

        assertThat(learningSessionService.getRequired(created.sessionId())).isEqualTo(created);
    }

    @Test
    void shouldRejectUnknownSessionId() {
        assertThatThrownBy(() -> learningSessionService.getRequired("missing"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
    }
}

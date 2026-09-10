package com.brightminds.rebuild.learning.session;

import com.brightminds.rebuild.common.response.ApiResponse;
import com.brightminds.rebuild.learning.chat.ChatMessageRequest;
import com.brightminds.rebuild.learning.chat.ChatStreamPayload;
import com.brightminds.rebuild.learning.chat.TutorChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/learning-sessions")
public class LearningSessionController {

    private final LearningSessionService learningSessionService;
    private final TutorChatService tutorChatService;

    public LearningSessionController(
            LearningSessionService learningSessionService,
            TutorChatService tutorChatService) {
        this.learningSessionService = learningSessionService;
        this.tutorChatService = tutorChatService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LearningSessionResponse> create(
            @Valid @RequestBody CreateLearningSessionRequest request) {
        return ApiResponse.created(learningSessionService.create(request));
    }

    @PostMapping(
            value = "/{sessionId}/messages:stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamPayload>> streamMessage(
            @PathVariable String sessionId,
            @Valid @RequestBody ChatMessageRequest request) {
        LearningSessionResponse session = learningSessionService.getRequired(sessionId);
        return tutorChatService.streamReply(session, request.message());
    }
}

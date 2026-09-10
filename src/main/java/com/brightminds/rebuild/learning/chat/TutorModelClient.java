package com.brightminds.rebuild.learning.chat;

import reactor.core.publisher.Flux;

public interface TutorModelClient {

    Flux<String> stream(TutorPrompt prompt);
}

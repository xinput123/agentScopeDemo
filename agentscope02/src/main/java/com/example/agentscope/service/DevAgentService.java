package com.example.agentscope.service;

import com.example.agentscope.dto.DevAgentEvent;
import com.example.agentscope.dto.DevAgentRequest;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DevAgentService {

    private final HarnessAgent devAgent;

    public DevAgentService(HarnessAgent devAgent) {
        this.devAgent = devAgent;
    }

    public Flux<DevAgentEvent> ask(DevAgentRequest request) {
        RuntimeContext.Builder contextBuilder = RuntimeContext.builder().sessionId(request.sessionId());
        if (request.userId() != null && !request.userId().isBlank()) {
            contextBuilder.userId(request.userId().strip());
        }

        RuntimeContext context = contextBuilder.build();
        Flux<DevAgentEvent> messages = devAgent
                .streamEvents(request.message(), context)
                .ofType(TextBlockDeltaEvent.class)
                .map(event -> DevAgentEvent.message(request.sessionId(), event.getDelta()));

        return Flux.concat(
                Mono.just(DevAgentEvent.session(request.sessionId())),
                messages,
                Mono.just(DevAgentEvent.done(request.sessionId())));
    }
}

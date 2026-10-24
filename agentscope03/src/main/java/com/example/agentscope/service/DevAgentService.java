package com.example.agentscope.service;

import com.example.agentscope.dto.DevAgentEvent;
import com.example.agentscope.dto.DevAgentRequest;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultEndEvent;
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
        RuntimeContext.Builder contextBuilder = RuntimeContext.builder()
                .sessionId(request.sessionId());
        if (request.userId() != null && !request.userId().isBlank()) {
            contextBuilder.userId(request.userId().strip());
        }
        RuntimeContext context = contextBuilder.build();

        Flux<DevAgentEvent> events = devAgent
                .streamEvents(request.message(), context)
                .handle((event, sink) -> {
                    DevAgentEvent mapped = mapEvent(request.sessionId(), event);
                    if (mapped != null) {
                        sink.next(mapped);
                    }
                });

        return Flux.concat(
                Mono.just(DevAgentEvent.session(request.sessionId())),
                events,
                Mono.just(DevAgentEvent.done(request.sessionId())));
    }

    private DevAgentEvent mapEvent(String sessionId, AgentEvent event) {
        return switch (event.getType()) {
            case AGENT_START, MODEL_CALL_START, AGENT_END -> lifecycleEvent(sessionId, event);
            case TEXT_BLOCK_DELTA -> textEvent(sessionId, (TextBlockDeltaEvent) event);
            case TOOL_CALL_START -> toolCallEvent(sessionId, (ToolCallStartEvent) event);
            case TOOL_RESULT_END -> toolResultEvent(sessionId, (ToolResultEndEvent) event);
            case AGENT_RESULT -> resultEvent(sessionId, (AgentResultEvent) event);
            default -> null;
        };
    }

    private DevAgentEvent lifecycleEvent(String sessionId, AgentEvent event) {
        return DevAgentEvent.agentEvent(
                event.getType().getValue(), sessionId, event.getId(), "");
    }

    private DevAgentEvent textEvent(String sessionId, TextBlockDeltaEvent event) {
        return DevAgentEvent.message(sessionId, event.getId(), event.getDelta());
    }

    private DevAgentEvent toolCallEvent(String sessionId, ToolCallStartEvent event) {
        return DevAgentEvent.namedEvent(
                "TOOL_CALL_START",
                sessionId,
                event.getId(),
                event.getToolCallId(),
                event.getToolCallName(),
                "准备调用工具：" + event.getToolCallName());
    }

    private DevAgentEvent toolResultEvent(String sessionId, ToolResultEndEvent event) {
        return DevAgentEvent.stateEvent(
                "TOOL_RESULT_END",
                sessionId,
                event.getId(),
                event.getToolCallId(),
                event.getToolCallName(),
                event.getState() == null ? null : event.getState().name());
    }

    private DevAgentEvent resultEvent(String sessionId, AgentResultEvent event) {
        return DevAgentEvent.agentEvent(
                "AGENT_RESULT",
                sessionId,
                event.getId(),
                event.getResult() == null ? "" : event.getResult().getTextContent());
    }
}

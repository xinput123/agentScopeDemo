package com.example.agentscope.service;

import com.example.agentscope.dto.DevAgentConfirmRequest;
import com.example.agentscope.dto.DevAgentEvent;
import com.example.agentscope.dto.DevAgentRequest;
import com.example.agentscope.dto.PendingToolCall;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEndEvent;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.event.AgentStartEvent;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.event.ModelCallEndEvent;
import io.agentscope.core.event.ModelCallStartEvent;
import io.agentscope.core.event.RequestStopEvent;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ToolCallEndEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultEndEvent;
import io.agentscope.core.event.ToolResultStartEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DevAgentService {

    @Resource
    private HarnessAgent devAgent;

    private final Map<ConfirmationKey, List<ToolUseBlock>> pendingConfirmations =
            new ConcurrentHashMap<>();

    public Flux<DevAgentEvent> ask(DevAgentRequest request) {
        RuntimeContext.Builder contextBuilder = RuntimeContext.builder().sessionId(request.sessionId());
        if (request.userId() != null && !request.userId().isBlank()) {
            contextBuilder.userId(request.userId().strip());
        }

        RuntimeContext context = contextBuilder.build();
        Flux<DevAgentEvent> events = devAgent
                .streamEvents(request.message(), context)
                .handle((event, sink) -> {
                    DevAgentEvent mapped =
                            toDevAgentEvent(
                                    request.userId(),
                                    request.sessionId(),
                                    event);
                    if (mapped != null) {
                        sink.next(mapped);
                    }
                });

        return Flux.concat(
                Mono.just(DevAgentEvent.session(request.sessionId())),
                events,
                Mono.just(DevAgentEvent.done(request.sessionId())));
    }

    public Flux<DevAgentEvent> confirm(DevAgentConfirmRequest request) {
        ConfirmationKey key = confirmationKey(request.userId(), request.sessionId());
        List<ToolUseBlock> pending = pendingConfirmations.remove(key);
        if (pending == null || pending.isEmpty()) {
            return Flux.just(
                    DevAgentEvent.agentEvent(
                            "ERROR",
                            request.sessionId(),
                            null,
                            "没有找到待确认的工具调用。"));
        }

        RuntimeContext.Builder contextBuilder = RuntimeContext.builder()
                .sessionId(request.sessionId());
        if (request.userId() != null && !request.userId().isBlank()) {
            contextBuilder.userId(request.userId().strip());
        }
        RuntimeContext context = contextBuilder.build();

        List<ConfirmResult> confirmResults = pending.stream()
                .map(toolCall -> new ConfirmResult(request.approved(), toolCall))
                .toList();
        Msg resumeMessage = Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .textContent(request.approved() ? "approved" : "denied")
                .metadata(Map.of(Msg.METADATA_CONFIRM_RESULTS, confirmResults))
                .build();

        Flux<DevAgentEvent> events = devAgent
                .streamEvents(List.of(resumeMessage), context)
                .handle((event, sink) -> {
                    DevAgentEvent mapped =
                            toDevAgentEvent(
                                    request.userId(),
                                    request.sessionId(),
                                    event);
                    if (mapped != null) {
                        sink.next(mapped);
                    }
                });

        return Flux.concat(
                Mono.just(DevAgentEvent.session(request.sessionId())),
                events,
                Mono.just(DevAgentEvent.done(request.sessionId())));
    }

    private DevAgentEvent toDevAgentEvent(
            String userId, String sessionId, AgentEvent event) {
        if (event instanceof AgentStartEvent e) {
            return DevAgentEvent.agentEvent("AGENT_START", sessionId, e.getId(), "");
        }
        if (event instanceof ModelCallStartEvent e) {
            return DevAgentEvent.agentEvent("MODEL_CALL_START", sessionId, e.getId(), "");
        }
        if (event instanceof TextBlockDeltaEvent e) {
            return DevAgentEvent.message(sessionId, e.getId(), e.getDelta());
        }
        if (event instanceof ToolCallStartEvent e) {
            return DevAgentEvent.namedEvent(
                    "TOOL_CALL_START",
                    sessionId,
                    e.getId(),
                    e.getToolCallId(),
                    e.getToolCallName(),
                    "准备调用工具：" + e.getToolCallName());
        }
        if (event instanceof ToolCallEndEvent e) {
            return DevAgentEvent.namedEvent(
                    "TOOL_CALL_END",
                    sessionId,
                    e.getId(),
                    e.getToolCallId(),
                    e.getToolCallName(),
                    "工具调用请求已生成：" + e.getToolCallName());
        }
        if (event instanceof ToolResultStartEvent e) {
            return DevAgentEvent.namedEvent(
                    "TOOL_RESULT_START",
                    sessionId,
                    e.getId(),
                    e.getToolCallId(),
                    e.getToolCallName(),
                    "开始处理工具结果：" + e.getToolCallName());
        }
        if (event instanceof ToolResultEndEvent e) {
            return DevAgentEvent.stateEvent(
                    "TOOL_RESULT_END",
                    sessionId,
                    e.getId(),
                    e.getToolCallId(),
                    e.getToolCallName(),
                    e.getState() == null ? null : e.getState().name());
        }
        if (event instanceof RequireUserConfirmEvent e) {
            pendingConfirmations.put(
                    confirmationKey(userId, sessionId),
                    e.getToolCalls());
            return DevAgentEvent.confirmation(
                    sessionId,
                    e.getId(),
                    e.getToolCalls().stream()
                            .map(this::toPendingToolCall)
                            .toList());
        }
        if (event instanceof RequestStopEvent e) {
            return DevAgentEvent.agentEvent(
                    "REQUEST_STOP",
                    sessionId,
                    e.getId(),
                    e.getGenerateReason().name() + ": " + e.getReason());
        }
        if (event instanceof ModelCallEndEvent e) {
            return DevAgentEvent.agentEvent("MODEL_CALL_END", sessionId, e.getId(), "");
        }
        if (event instanceof AgentResultEvent e) {
            return DevAgentEvent.agentEvent(
                    "AGENT_RESULT",
                    sessionId,
                    e.getId(),
                    e.getResult() == null ? "" : e.getResult().getTextContent());
        }
        if (event instanceof AgentEndEvent e) {
            return DevAgentEvent.agentEvent("AGENT_END", sessionId, e.getId(), "");
        }
        return null;
    }

    private PendingToolCall toPendingToolCall(ToolUseBlock toolCall) {
        return new PendingToolCall(
                toolCall.getId(),
                toolCall.getName(),
                toolCall.getInput());
    }

    private ConfirmationKey confirmationKey(String userId, String sessionId) {
        String normalizedUserId =
                userId == null || userId.isBlank()
                        ? "__anonymous__"
                        : userId.strip();
        return new ConfirmationKey(normalizedUserId, sessionId.strip());
    }

    private record ConfirmationKey(String userId, String sessionId) {
    }
}

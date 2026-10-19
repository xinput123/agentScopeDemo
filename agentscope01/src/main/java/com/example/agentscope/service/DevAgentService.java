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
        // Service 里把 sessionId 和可选的 userId 放进 RuntimeContext，再从事件流里取出
        // 普通模型接口更多是在处理这次 Prompt。
        // HarnessAgent 会根据 RuntimeContext 里的 (userId, sessionId) 找到对应的 Agent 状态。
        // 同一个 HarnessAgent Bean 可以服务多个用户、多段会话，不需要每个用户 new 一个 Agent。
        RuntimeContext.Builder contextBuilder =
                RuntimeContext.builder().sessionId(request.sessionId());
        if (request.userId() != null && !request.userId().isBlank()) {
            contextBuilder.userId(request.userId().strip());
        }
        RuntimeContext context = contextBuilder.build();

        // streamEvents() 返回 Flux<AgentEvent>。一次执行除了文本，还可能产生工具调用、工具结果和确认请求等事件。
        // 这个接口没有工具，只取 TextBlockDeltaEvent，把模型生成的文字逐段送给前端
        Flux<DevAgentEvent> messages = devAgent
                .streamEvents(request.message(), context)
                .ofType(TextBlockDeltaEvent.class)
                .map(event -> DevAgentEvent.message(
                        request.sessionId(),
                        event.getDelta()));

        return Flux.concat(
                Mono.just(DevAgentEvent.session(request.sessionId())),
                messages,
                Mono.just(DevAgentEvent.done(request.sessionId())));
    }
}

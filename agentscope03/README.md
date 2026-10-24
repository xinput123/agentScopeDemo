# AgentScope Java 2.0: 让前端看见 Agent 调工具的过程

接口已经能流式返回文字了。

但用户问：

```text
帮我看一下这个项目用了哪个 Java 版本、Spring Boot 版本，
启动类在哪里，以及源码目录有哪些
```

Agent 会先调用 read_pom、list_source_folders 和 find_main_class，再根据工具结果生成回答。

如果后端只把文字分片推给前端，页面只能显示“正在生成”，看不到 Agent 正在调用哪个工具，也不知道工具是否执行成功。

AgentScope Java 2.0 的 streamEvents() 返回：

```java
Flux<AgentEvent>
```

这里不只有文字，还包括模型调用、工具调用、工具结果和 Agent 结束等事件。

## 一、一次工具调用会产生哪些事件

当前 Web 接口筛选后的主要事件顺序如下：

```text
AgentStartEvent
-> ModelCallStartEvent
-> ToolCallStartEvent
-> ToolResultEndEvent
-> ModelCallStartEvent
-> TextBlockDeltaEvent（模型返回文字增量时）
-> AgentResultEvent
-> AgentEndEvent
```

两次 ModelCallStartEvent 分别对应：

```text
第一次：模型选择工具
第二次：模型读取工具结果，生成回答
```

工具事件要分清两个阶段：

```text
ToolCall：模型生成工具调用请求
ToolResult：Java 工具执行并返回结果
```

ToolCallStartEvent 表示模型响应里已经出现工具调用请求，并不代表 Java 工具已经开始执行。真正进入执行阶段时，
框架还会产生 ToolResultStartEvent，当前接口没有转发这个事件。

页面要判断工具是否执行、结果如何，应看 ToolResultEndEvent.state：

```text
SUCCESS
ERROR
DENIED
INTERRUPTED
RUNNING
```

RUNNING 表示工具调用被挂起，当前还没有完成。

接口只保留“准备调用”和“执行结束”两个节点。更细的参数增量、结果分片不再转发。

#### eventId 和 toolCallId

每条 AgentEvent 都有自己的 eventId。同一次工具调用会跨过多条事件，因此还需要 toolCallId：

```text
ToolCallStartEvent
eventId: event-101
toolCallId: call-read-pom

ToolResultEndEvent
eventId: event-205
toolCallId: call-read-pom
```

前端通过 toolCallId 找到同一条工具记录，把状态从“准备调用”更新为 SUCCESS。

```text
eventId：标识一条事件
toolCallId：关联一次工具调用
```

## 二、接口定义自己的事件结构

直接返回框架的 AgentEvent，前端会和 AgentScope Java 的事件类绑在一起。
业务侧定义一个简单的 SSE DTO：

```java

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DevAgentEvent(
        String type,
        String sessionId,
        String content,
        String eventId,
        String toolCallId,
        String name,
        String state) {
}
```

字段分工如下：

```text
type：事件类型
sessionId：所属会话
eventId：
当前事件 ID
toolCallId：
工具调用 ID
name：工具名
state：工具执行状态
content：文本分片或最终回答
```

工具开始时，前端会收到：

```text
{
  "type": "TOOL_CALL_START",
  "sessionId": "event-session-001",
  "toolCallId": "<tool-call-id>",
  "name": "read_pom",
  "content": "准备调用工具：read_pom"
}
```

工具结束后，使用同一个 toolCallId 更新状态：

```json
{
  "type": "TOOL_RESULT_END",
  "sessionId": "event-session-001",
  "toolCallId": "<tool-call-id>",
  "name": "read_pom",
  "state": "SUCCESS",
  "content": ""
}
```

## 三、把 AgentEvent 转成业务事件

Service 订阅 streamEvents()，只转发页面需要的事件：

```text
Flux<DevAgentEvent> events = devAgent
        .streamEvents(request.message(), context)
        .handle((event, sink) -> {
            DevAgentEvent mapped =
                    mapEvent(
                            request.sessionId(), event);
            if (mapped != null) {
                sink.next(mapped);
            }
        });

return Flux.concat(
        Mono.just(
                DevAgentEvent.session(
                request.sessionId())),
        events,
        Mono.just(
        DevAgentEvent.done(
                request.sessionId())));
```

mapEvent() 按事件类型分发：

```text
private DevAgentEvent mapEvent(
        String sessionId, AgentEvent event) {
    return switch (event.getType()) {
        case AGENT_START,
             MODEL_CALL_START,
             AGENT_END ->
                lifecycleEvent(sessionId, event);
        case TEXT_BLOCK_DELTA ->
                textEvent(
                        sessionId,
                        (TextBlockDeltaEvent) event);
        case TOOL_CALL_START ->
                toolCallEvent(
                        sessionId,
                        (ToolCallStartEvent) event);
        case TOOL_RESULT_END ->
                toolResultEvent(
                        sessionId,
                        (ToolResultEndEvent) event);
        case AGENT_RESULT ->
                resultEvent(
                        sessionId,
                        (AgentResultEvent) event);
        default -> null;
    };
}
```

例如工具结果的转换：

```text
private DevAgentEvent toolResultEvent(
        String sessionId,
        ToolResultEndEvent event) {
    return DevAgentEvent.stateEvent(
            "TOOL_RESULT_END",
            sessionId,
            event.getId(),
            event.getToolCallId(),
            event.getToolCallName(),
            event.getState() == null
                    ? null
                    : event.getState().name());
}
```

没有列在 switch 中的事件会被跳过。

接口同时处理 MESSAGE 和 AGENT_RESULT：

```text
MESSAGE：模型适配器产生文字增量时，用来实时更新页面
AGENT_RESULT：本次调用的完整结果，用来保存最终回答
```

AGENT_RESULT 不要再次追加到聊天窗口，否则回答会显示两遍。

## 四、用 curl 看实际事件流

Controller 返回 text/event-stream：

```java

@PostMapping(
        path = "/ask",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<DevAgentEvent> ask(
        @Valid @RequestBody DevAgentRequest request) {
    return devAgentService.ask(request);
}
```

调用接口：

```shell
curl -sN -X POST \
  "http://localhost:8080/dev-agent/ask" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "dev-user-001",
    "sessionId": "event-session-001",
    "message": "帮我看一下这个项目用了哪个 Java 版本、Spring Boot 版本，启动类在哪里，以及源码目录有哪些"
  }'
```

这次测试记录的主要顺序如下：

```text
SESSION
AGENT_START
MODEL_CALL_START

TOOL_CALL_START  read_pom
TOOL_CALL_START  list_source_folders
TOOL_CALL_START  find_main_class

TOOL_RESULT_END  read_pom             SUCCESS
TOOL_RESULT_END  list_source_folders  SUCCESS
TOOL_RESULT_END  find_main_class      SUCCESS

MODEL_CALL_START
AGENT_RESULT
AGENT_END
DONE
```

三个工具分别读取：

```text
read_pom：pom.xml
list_source_folders：src 目录
find_main_class：@SpringBootApplication 启动类
```

SESSION 和 DONE 是当前 Web 接口自己添加的：

```text
SESSION：会话开始
DONE：SSE 流正常发送完毕
```

AGENT_END 是 Agent 的生命周期事件，DONE 是当前接口补充的正常结束信号。上游发生异常时，现有代码不会继续追加 DONE。

当前模型适配器如果产生 TextBlockDeltaEvent，中间还会出现 MESSAGE；无论是否有文字分片，成功结束时都可以从 AGENT_RESULT 取得完整结果。

## 写在最后

TextBlockDeltaEvent 用来显示回答，ToolCallStartEvent 告诉页面模型准备调用什么工具，ToolResultEndEvent 给出执行结果，AgentResultEvent 保存最终回答。

后端把这些事件转成自己的 SSE 协议，前端才能看清 Agent 的执行过程。

生产接口还要补充业务错误事件和 traceId，异常堆栈留在服务端日志中。
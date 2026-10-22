## Toolkit 实战
模型懂 Java，不代表它知道你这个项目。

问它“项目用了哪个 Java 版本”，没有工具时，它看不到当前项目的 pom.xml，只能根据已有上下文回答。

这次给 Agent 加三个只读工具：
```text
read_pom：读取 pom.xml
list_source_folders：查看源码目录
find_main_class：查找 Spring Boot 启动类
```

这篇顺着一条链路往下拆：Java 方法怎样注册进 Toolkit，Toolkit 怎样把它变成模型能理解的 Schema，模型选中工具后，应用又怎样完成执行。

## 一、一个方法怎样进入 Toolkit
普通方法不会自动出现在模型的工具列表里。

AgentScope Java 的注解式工具会经过这几步：
```text
@Tool 方法
-> Toolkit.registerTool(...)
-> 包装成 ReflectiveFunctionTool
-> 注册进 Toolkit
```
registerTool(Object) 内部会反射扫描对象的方法，只处理带 @Tool 的部分。核心逻辑可以压缩成下面几行：
```java
for (Method method : toolObject.getClass().getDeclaredMethods()) {
    if (method.isAnnotationPresent(Tool.class)) {
        registerToolMethod(toolObject, method, ...);
    }
}
```
registerToolMethod(...) 不会把 Method 直接存进一个 Map，而是把它包装成 ReflectiveFunctionTool：
```java
AgentTool tool = ReflectiveFunctionTool.create(...);
registerAgentTool(tool, ...);
```

可以把 ReflectiveFunctionTool 看成一个转接层：
```text
模型这一侧：只认识工具名、说明和参数
Java 这一侧：真正执行的是对象和方法
```
还是以 read_pom 为例。注册前，它只是 ProjectInfoTools 里的一个方法：
```java
String readPom(Integer maxChars)
```
注册后，ReflectiveFunctionTool 会保存：
```java
工具实现：ProjectInfoTools 对象 + readPom 方法
工具配置：name、description、readOnly、concurrencySafe、参数 Schema
```
Agent 运行时不需要专门认识 ProjectInfoTools，只需要按统一的 AgentTool 接口处理。

到这里，read_pom 已经进入 Toolkit，应用可以按工具名找到它。但模型还看不懂 Java 方法，下一步还要生成 ToolSchema。

## 二、Toolkit 怎么把工具变成 Schema
注册完成后，Toolkit 保存的是可执行工具，不会把 Java 对象直接发给模型。每次调用模型前，它会临时生成一份 List<ToolSchema>。

Toolkit 内部由三个组件分工：
```text
ToolRegistry：保存已经注册的工具
ToolSchemaProvider：把工具整理成 ToolSchema
ToolExecutor：执行模型选中的工具
```
ToolSchemaProvider 是 Toolkit 的内部组件，业务代码不需要直接调用。Toolkit 创建时，会把自己的 ToolRegistry 和 ToolGroupManager 交给它：
```java
private final ToolRegistry toolRegistry = new ToolRegistry();
private final ToolGroupManager groupManager = new ToolGroupManager();
private final ToolSchemaProvider schemaProvider;

this.schemaProvider = new ToolSchemaProvider(toolRegistry, groupManager);
```
需要把工具发给模型时，外部仍然调用 Toolkit：
```java
public List<ToolSchema> getToolSchemas() {
    return schemaProvider.getToolSchemas();
}
```
ToolSchemaProvider 再从已注册工具中取出名称、描述和参数，生成模型能识别的 ToolSchema：
```java
ToolSchema.builder()
        .name(toolName)
        .description(tool.getDescription())
        .parameters(registered.getExtendedParameters())
        .strict(tool.getStrict())
        .outputSchema(tool.getOutputSchema())
        .build();
```
模型根据 Schema 判断该选哪个工具：
```text
问 Java 或 Spring Boot 版本 -> read_pom
问源码目录怎么分布         -> list_source_folders
问启动类在哪里             -> find_main_class
```
所以 description 不能都写成“读取项目信息”。它会进入模型请求，直接影响模型能不能选对工具。

模型决定调用 read_pom 后，AgentScope 会把模型返回的工具调用整理成 ToolUseBlock。关键信息相当于：
```json
{
  "name": "read_pom",
  "input": {
    "max_chars": 2000
  }
}
```

后面的执行过程是：
```text
按名称找到 read_pom
-> 检查这次调用是否允许
-> 校验并转换 max_chars
-> 调用 projectInfoTools.readPom(2000)
-> 把读取结果交回模型
```
PermissionEngine 负责权限判断，ToolExecutor 处理参数检查和执行调度。ReflectiveFunctionTool 把调用交给 ToolMethodInvoker，由它执行对应的 Java 方法。

如果同一轮有多个工具调用，是否并行先由 ToolkitConfig.parallel 决定，默认按顺序执行。开启并行后，ToolExecutor 再根据 concurrencySafe 安排哪些调用可以并发、哪些必须串行。需要用户确认的工具会先停下来，不会直接调用 Java 方法。

## 三、把项目读取能力封成三个工具
ProjectInfoTools 接收一个固定的项目根目录，不让模型传入任意路径。

先看 read_pom：
```java
public class ProjectInfoTools {
    
    private final Path projectRoot;
    
    public ProjectInfoTools(Path projectRoot) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
    }
    
    @Tool(
            name = "read_pom",
            description = "Read the current Maven project's pom.xml. Use it when the user asks about dependencies, Java version, Spring Boot version, or project coordinates.",
            readOnly = true)
    public String readPom(
            @ToolParam(
                    name = "max_chars",
                    description = "Maximum characters to return. Default is 4000.",
                    required = false)
            Integer maxChars) {
        // 读取 projectRoot/pom.xml，并按 max_chars 截断
    }
}
```
readOnly = true 表示这个工具没有写入副作用。@ToolParam 的 name 需要明确填写，因为 Java 运行时不保证能拿到可靠的参数名。concurrencySafe 默认是 true，只有工具不能并发执行时才需要改成 false。

另外两个工具写法一样：
```java
@Tool(
        name = "list_source_folders",
        description = "List existing source folders in the current project. Use it before answering questions about project layout.",
        readOnly = true)
public String listSourceFolders() {
    // 查看 projectRoot/src 下的目录
}

@Tool(
        name = "find_main_class",
        description = "Find Java classes annotated with @SpringBootApplication in the current project.",
        readOnly = true)
public String findMainClass() {
    // 查找 src/main/java 下的 Spring Boot 启动类
}
```
这三个工具都不接收文件路径，不执行命令，也不修改文件。模型只能调用代码提前划定好的能力。

## 四、哪些参数能让模型自己填
read_pom 运行时需要两类信息：
```text
projectRoot：去哪个项目里找 pom.xml
max_chars：最多返回多少字符
```
这两个值不能都交给模型。

projectRoot 在应用启动时从 application.yml 读取，并传给 ProjectInfoTools：
```java
new ProjectInfoTools(Path.of(properties.projectRoot()))
```
ProjectInfoTools 是 Spring Bean，所以当前示例中的所有会话共用这个项目目录。它没有使用 @ToolParam，不会出现在 Tool Schema 中，模型也不能修改。

当前 Demo 是单项目模式。调用 /dev-agent/ask 时只传 userId、sessionId 和问题，不传 projectRoot。

如果以后要支持多个项目，可以由服务端根据用户、会话或任务选择项目目录，再把结果放进运行上下文。项目路径仍不建议作为 @ToolParam 交给模型。

max_chars 使用了 @ToolParam：

```java
public String readPom(
        @ToolParam(name = "max_chars", required = false)
        Integer maxChars) {
    // ...
}
```
这个参数会进入 Tool Schema。模型调用 read_pom 时，可以根据需要填写 2000 或 4000。
边界就清楚了：
```text
服务端决定读哪里

模型决定返回多少
```
如果把文件路径也声明成 @ToolParam，模型就能提交任意路径，工具的能力会从“读取当前项目的 pom.xml”扩大成“读取指定文件”。所以，目录、租户和用户身份这类边界参数应该留在应用侧。

## 五、三个工具为什么都能被模型看到
当前 Agent 使用 EXPLORE 权限模式，并把 ProjectInfoTools 注册进自己的 Toolkit：
```java
HarnessAgent agent = HarnessAgent.builder()
        // 其余配置保持不变
        .permissionContext(PermissionContextState.builder()
                .mode(PermissionMode.EXPLORE)
                .build())
        .build();

agent.getToolkit().registerTool(projectInfoTools);
```
这里没有指定 Tool Group，三个工具都属于未分组工具。未分组工具不会被过滤，因此每次调用模型时，它们都会出现在 List<ToolSchema> 中。

工具多起来以后，可以按用途分组。例如：
```text
project_config：read_pom
project_structure：list_source_folders、find_main_class
```
如果把三个工具按上面的方式分组，当前会话只激活 project_config，模型就只能看到 read_pom。

ReActAgent 会把当前会话激活的组名交给 Toolkit：
```java
toolkit.getToolSchemas(state.getToolContext().getActivatedGroups())
```

激活的组名保存在会话的 ToolContextState 中，不同会话可以使用不同的工具集合。

Tool Group 和权限解决的是两个问题：
```text
Tool Group：哪些工具会进入本次 List<ToolSchema>
Permission：模型选中工具后，这次调用能不能执行
```
当前三个工具都标记了 readOnly = true，所以在 EXPLORE 模式下可以执行。修改类工具即使出现在 Schema 中，也会被拒绝。

Prompt 再补充什么时候调用工具，以及目前没有哪些能力：
```yaml
app:
  dev-agent:
    project-root: .
    system-prompt: |
      当用户询问项目依赖、Java 版本、Spring Boot 版本、源码结构或启动类时，
      先调用对应工具，再基于工具结果回答。
      当前没有接入日志、数据库、写文件或 Shell 工具，
      不要声称已经完成查询日志、修改代码、保存文件或执行命令。
```
project-root 决定工具能读哪里，Prompt 告诉模型什么时候使用这些能力。路径限制留在代码里，不能只靠提示词约束。

## 六、curl 看实际结果
接口继续使用 /dev-agent/ask：
```shell
curl -sN -X POST "http://localhost:8080/dev-agent/ask" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "dev-user-001",
    "sessionId": "toolkit-session-001",
    "message": "帮我看一下这个项目用了哪个 Java 版本、Spring Boot 版本，以及启动类在哪里"
  }'
```
实际测试时，SSE 会把回答拆成很多个 MESSAGE。下面是原始返回节选：
```text
data:{"type":"SESSION","sessionId":"toolkit-session-001","content":""}

data:{"type":"MESSAGE","sessionId":"toolkit-session-001","content":"Java"}

data:{"type":"MESSAGE","sessionId":"toolkit-session-001","content":"版本"}

data:{"type":"MESSAGE","sessionId":"toolkit-session-001","content":"："}

data:{"type":"MESSAGE","sessionId":"toolkit-session-001","content":"17"}

...

data:{"type":"MESSAGE","sessionId":"toolkit-session-001","content":"Spring"}

data:{"type":"MESSAGE","sessionId":"toolkit-session-001","content":" Boot"}

data:{"type":"MESSAGE","sessionId":"toolkit-session-001","content":"4"}

data:{"type":"MESSAGE","sessionId":"toolkit-session-001","content":"."}

data:{"type":"MESSAGE","sessionId":"toolkit-session-001","content":"1"}

data:{"type":"MESSAGE","sessionId":"toolkit-session-001","content":"."}

data:{"type":"MESSAGE","sessionId":"toolkit-session-001","content":"0"}

...

data:{"type":"MESSAGE","sessionId":"toolkit-session-001","content":"AgentScope"}

data:{"type":"MESSAGE","sessionId":"toolkit-session-001","content":"Java"}

data:{"type":"MESSAGE","sessionId":"toolkit-session-001","content":"Application"}

data:{"type":"MESSAGE","sessionId":"toolkit-session-001","content":".java"}

data:{"type":"DONE","sessionId":"toolkit-session-001","content":""}
```

把所有 MESSAGE.content 按顺序拼接后，完整回答是：
```text
1. Java 版本：17，在 pom.xml 的 <java.version> 属性中声明。
2. Spring Boot 版本：4.1.0，继承自 spring-boot-starter-parent。
3. 启动类位置：src/main/java/com/example/agentscope/AgentScopeJavaApplication.java，
   包含 @SpringBootApplication 注解。
```

Java 版本和 Spring Boot 版本由 read_pom 从 pom.xml 中读取，启动类路径由 find_main_class 从源码目录
中查找。Prompt 只告诉模型什么时候调用工具，没有预先写入这些结果。

## 写在最后
给 Java 方法加上 @Tool，只是完成了工具定义。它能不能出现在本次请求的工具列表里、模型选中后是否允许调用、最终由哪个 Java 方法执行，仍由 Toolkit 和应用侧规则控制。

模型可以提出“调用 read_pom”，但读取范围、参数限制和执行权限不能交给模型决定。这才是 Toolkit 在工程里的作用：把模型的工具选择，变成一次有边界、能落地的 Java 调用。
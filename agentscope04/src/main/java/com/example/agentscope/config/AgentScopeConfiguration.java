package com.example.agentscope.config;

import com.example.agentscope.tool.FileChangeTool;
import com.example.agentscope.tool.ProjectInfoTools;
import io.agentscope.core.model.Model;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.formatter.DeepSeekFormatter;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Configuration
public class AgentScopeConfiguration {

    private static final List<String> READ_ONLY_TOOL_NAMES =
            List.of("read_pom", "list_source_folders", "find_main_class");

    @Bean
    Model deepSeekModel(DevAgentProperties properties) {
        DevAgentProperties.Model model = properties.model();
        return OpenAIChatModel.builder()
                .apiKey(model.apiKey())
                .baseUrl(model.baseUrl())
                .modelName(model.name())
                .formatter(new DeepSeekFormatter())
                .stream(true)
                .build();
    }

    @Bean
    HarnessAgent devAgent(
            Model deepSeekModel,
            DevAgentProperties properties,
            ProjectInfoTools projectInfoTools,
            FileChangeTool fileChangeTool)
            throws IOException {
        HarnessAgent agent = HarnessAgent.builder()
                .name(properties.name())
                .sysPrompt(properties.systemPrompt())
                .model(deepSeekModel)
                .stateStore(new InMemoryAgentStateStore())
                .permissionContext(permissionContext())
                .enableAgentTracingLog(false)
                .disableFilesystemTools()
                .disableShellTool()
                .disableMemoryTools()
                .disableMemoryHooks()
                .disableCompaction()
                .disableSubagents()
                .disableWorkspaceContext()
                .disableAtPathExpansion()
                .disableDynamicSkills()
                .disableDefaultWorkspaceSkills()
                .disableToolsConfig()
                .build();

        agent.getToolkit().registerTool(projectInfoTools);
        // 将工具注册进原来的 Toolkit
        agent.getToolkit().registerAgentTool(fileChangeTool);
        agent.getToolkit().removeTool("wait_async_results");
        return agent;
    }

    @Bean
    ProjectInfoTools projectInfoTools(DevAgentProperties properties) {
        return new ProjectInfoTools(Path.of(properties.projectRoot()));
    }

    @Bean
    FileChangeTool fileChangeTool(DevAgentProperties properties) {
        return new FileChangeTool(Path.of(properties.projectRoot()));
    }

    private PermissionContextState permissionContext() {
        PermissionContextState.Builder builder =
                PermissionContextState.builder()
                        // PermissionMode.EXPLORE，它会允许只读工具、拒绝修改类工具
                        // 要让文件写入进入人工确认，而不是直接被拒绝,权限模式改为 DEFAULT，再给三个只读工具配置 ALLOW
                        // DEFAULT 不会因为工具标记了 readOnly = true 就自动放行。没有命中规则，也没有被工具自身明确允许或拒绝时，默认结果是 ASK。
                        .mode(PermissionMode.DEFAULT);
        // READ_ONLY_TOOL_NAMES 是应用定义的白名单，不是 AgentScope 自动生成的。列表中的工具可以直接执行，其他工具继续走权限判断。
        READ_ONLY_TOOL_NAMES.forEach(
                toolName -> builder.addAllowRule(toolName, allowRule(toolName)));
        return builder.build();
    }

    private PermissionRule allowRule(String toolName) {
        return new PermissionRule(toolName, null, PermissionBehavior.ALLOW, "article-demo");
    }
}

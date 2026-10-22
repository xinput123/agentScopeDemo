package com.example.agentscope.config;

import com.example.agentscope.tool.ProjectInfoTools;
import io.agentscope.core.model.Model;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.formatter.DeepSeekFormatter;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 创建 HarnessAgent
 */
@Configuration
public class AgentScopeConfiguration {

    @Bean
    public Model deepSeekModel(DevAgentProperties properties) {
        DevAgentProperties.Model model = properties.model();
        return OpenAIChatModel.builder()
                .apiKey(model.apiKey())
                .baseUrl(model.baseUrl())
                .modelName(model.name())
                .formatter(new DeepSeekFormatter())
                .stream(true)
                .build();
    }

    /**
     * 创建 HarnessAgent
     */
    @Bean
    public HarnessAgent devAgent(
            Model deepSeekModel,
            DevAgentProperties properties,
            ProjectInfoTools projectInfoTools)
            throws IOException {
        HarnessAgent agent = HarnessAgent.builder()
                .name(properties.name())
                .sysPrompt(properties.systemPrompt())
                .model(deepSeekModel)
                .stateStore(new InMemoryAgentStateStore())
                .permissionContext(PermissionContextState.builder()
                        .mode(PermissionMode.EXPLORE)
                        .build())
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
        agent.getToolkit().removeTool("wait_async_results");
        return agent;
    }

    @Bean
    public ProjectInfoTools projectInfoTools(DevAgentProperties properties) {
        return new ProjectInfoTools(Path.of(properties.projectRoot()));
    }
}
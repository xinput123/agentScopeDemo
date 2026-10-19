package com.example.agentscope.config;

import io.agentscope.core.model.Model;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.formatter.DeepSeekFormatter;
import io.agentscope.harness.agent.HarnessAgent;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 创建一个 HarnessAgent
 */
@Configuration
public class AgentScopeConfiguration {

    /**
     * 先把 DeepSeek 模型包装成 AgentScope 的 Model
     */
    @Bean
    public Model deepSeekModel(DevAgentProperties properties) {
        DevAgentProperties.Model model = properties.model();
        return OpenAIChatModel.builder()
                .apiKey(model.apiKey())
                .baseUrl(model.baseUrl())
                .modelName(model.name())
                .formatter(new DeepSeekFormatter())
                .stream(true) // 打开流式输出，后面的 Web 接口才能边生成边返回
                .build();
    }

    /**
     * 再创建 HarnessAgent
     */
    @Bean
    public HarnessAgent devAgent(Model deepSeekModel, DevAgentProperties properties) throws IOException {
        HarnessAgent agent = HarnessAgent.builder()
                .name(properties.name())
                .sysPrompt(properties.systemPrompt())
                .model(deepSeekModel)
                .stateStore(new InMemoryAgentStateStore())
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

        agent.getToolkit().removeTool("wait_async_results");
        return agent;
    }
}

package com.example.agentscope;

import com.example.agentscope.config.DevAgentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * https://mp.weixin.qq.com/s/RJq3el6CMNoWM__cUXtahg
 */
@SpringBootApplication
@EnableConfigurationProperties(DevAgentProperties.class)
public class AgentScopeApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentScopeApplication.class, args);
    }
}



package com.example.agentscope;

import com.example.agentscope.config.DevAgentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * https://mp.weixin.qq.com/s/jM-kl4a5VLSJUV2hfZJ1pA
 */
@SpringBootApplication
@EnableConfigurationProperties(DevAgentProperties.class)
public class AgentScopeApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentScopeApplication.class, args);
    }
}



package com.example.agentscope.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.dev-agent")
public record DevAgentProperties(
        @NotBlank String name,
        @NotBlank String systemPrompt,
        @Valid Model model) {

    public record Model(
            @NotBlank String apiKey,
            @NotBlank String baseUrl,
            @NotBlank String name) {
    }
}

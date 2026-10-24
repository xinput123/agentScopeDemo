package com.example.agentscope.controller;

import com.example.agentscope.dto.DevAgentEvent;
import com.example.agentscope.dto.DevAgentRequest;
import com.example.agentscope.service.DevAgentService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/dev-agent")
public class DevAgentController {

    @Resource
    private DevAgentService devAgentService;

    @PostMapping(path = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<DevAgentEvent> ask(@Valid @RequestBody DevAgentRequest request) {
        return devAgentService.ask(request);
    }
}


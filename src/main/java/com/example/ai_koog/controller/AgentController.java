package com.example.ai_koog.controller;

import com.example.ai_koog.records.PersonEligibilityResult;
import com.example.ai_koog.service.AgentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/api/agent/messages")
    public PersonEligibilityResult sendMessage(@RequestBody ChatRequest request) {
        return agentService.ask(request.message());
    }

    public record ChatRequest(String message) {
    }
}

package com.ecomai.ecommerce.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ecomai.ecommerce.dto.AgentChatRequest;
import com.ecomai.ecommerce.dto.AgentResumeRequest;

public interface CustomerServiceAgentClient {

    JsonNode chat(AgentChatRequest request);

    JsonNode resume(AgentResumeRequest request);
}

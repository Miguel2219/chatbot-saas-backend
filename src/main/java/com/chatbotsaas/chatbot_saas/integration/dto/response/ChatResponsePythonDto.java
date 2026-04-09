package com.chatbotsaas.chatbot_saas.integration.dto.response;

import com.chatbotsaas.chatbot_saas.lead.dto.response.LeadDataDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatResponsePythonDto {

    @JsonProperty(value = "response")
    private String response;

    @JsonProperty(value = "lead_captured")
    private Boolean leadCaptured;

    @JsonProperty(value = "lead_data")
    private LeadDataDto leadData;

    @JsonProperty(value = "request_detail")
    private String requestDetail;

    @JsonProperty(value = "cede_control")
    private Boolean cedeControl;
}

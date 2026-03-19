package com.chatbotsaas.chatbot_saas.lead.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class LeadDataDto {

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "phone")
    private String phone;

    @JsonProperty(value = "email")
    private String email;

}

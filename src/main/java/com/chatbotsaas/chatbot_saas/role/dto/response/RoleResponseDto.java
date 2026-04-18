package com.chatbotsaas.chatbot_saas.role.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Builder
@Getter
@AllArgsConstructor
public class RoleResponseDto {

    @JsonProperty("role_id")
    private UUID roleId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;
}

package com.chatbotsaas.chatbot_saas.permission.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionCheckDto {

    @JsonProperty("permission_id")
    private UUID permissionId;

    @JsonProperty("action")
    private String action;

    @JsonProperty("granted")
    private Boolean granted;
}

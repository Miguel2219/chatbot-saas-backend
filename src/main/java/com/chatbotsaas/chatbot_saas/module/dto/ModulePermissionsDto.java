package com.chatbotsaas.chatbot_saas.module.dto;

import com.chatbotsaas.chatbot_saas.permission.dto.PermissionCheckDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModulePermissionsDto {

    @JsonProperty("module_id")
    private UUID moduleId;

    @JsonProperty("module_name")
    private String moduleName;

    @JsonProperty("icon")
    private String icon;

    @JsonProperty("permissions")
    private List<PermissionCheckDto> permissions;
}
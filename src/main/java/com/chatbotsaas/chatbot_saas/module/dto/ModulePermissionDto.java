package com.chatbotsaas.chatbot_saas.module.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.Map;
import java.util.UUID;


@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ModulePermissionDto {

    @JsonProperty("module_id")
    private UUID moduleId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("route")
    private String route;

    @JsonProperty("icon")
    private String icon;

    @JsonProperty("display_order")
    private Integer displayOrder;

    @JsonProperty("permissions")
    private Map<String, Boolean> permissions;
}

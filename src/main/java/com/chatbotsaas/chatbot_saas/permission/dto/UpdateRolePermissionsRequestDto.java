package com.chatbotsaas.chatbot_saas.permission.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRolePermissionsRequestDto {

    @JsonProperty("permission_ids")
    private List<UUID> permissionsIds;
}

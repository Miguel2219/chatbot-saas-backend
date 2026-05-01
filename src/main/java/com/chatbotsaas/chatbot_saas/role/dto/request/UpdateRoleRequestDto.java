package com.chatbotsaas.chatbot_saas.role.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequestDto {

    @NotBlank
    @Size(max = 50)
    @JsonProperty("name")
    private String name;

    @Size(max = 255)
    @JsonProperty("description")
    private String description;
}

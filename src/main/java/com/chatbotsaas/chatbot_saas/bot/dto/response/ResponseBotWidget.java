package com.chatbotsaas.chatbot_saas.bot.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Response del endpoint público {@code GET /api/bot/get_widget_bot/{bot_id}}.
 *
 * <p>Expone solo lo mínimo que el widget embebido necesita para renderizar
 * su UI inicial — hoy únicamente el nombre. Si en el futuro el widget
 * requiere más campos (color primario, avatar, mensaje de bienvenida, etc.),
 * se agregan acá sin tocar el response interno {@code ResponseBotDto} que
 * usa el panel autenticado.
 *
 * <p>Mantenemos un DTO separado de {@code ResponseBotDto} para evitar
 * exponer en endpoint público campos sensibles como {@code system_prompt},
 * {@code tenant_id} o lista de {@code lead_assignees}.
 */
@Getter
@Builder
public class ResponseBotWidget {

    @JsonProperty("name")
    private String name;
}

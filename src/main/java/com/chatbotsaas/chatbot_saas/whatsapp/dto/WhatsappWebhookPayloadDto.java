package com.chatbotsaas.chatbot_saas.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsappWebhookPayloadDto {

    @JsonProperty("object")
    private String object;

    @JsonProperty("entry")
    private List<Entry> entry;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry {
        /** WABA ID — útil para logs cuando un tenant tiene varias WABAs. */
        @JsonProperty("id")
        private String id;

        @JsonProperty("changes")
        private List<Change> changes;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Change {
        @JsonProperty("value")
        private ChangeValue value;

        @JsonProperty("field")
        private String field;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChangeValue {
        /** Siempre "whatsapp" en Cloud API. */
        @JsonProperty("messaging_product")
        private String messagingProduct;

        @JsonProperty("metadata")
        private WhatsappMetadata metadata;

        /**
         * Lista de contactos del remitente. Lo usamos en
         * {@code WhatsappService} para resolver el nombre del cliente
         * cuando el bot cede control y todavía no hay {@code Lead}
         * capturado.
         */
        @JsonProperty("contacts")
        private List<WhatsappContact> contacts;

        @JsonProperty("messages")
        private List<WhatsappMessage> messages;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhatsappMetadata {
        @JsonProperty("display_phone_number")
        private String displayPhoneNumber;

        @JsonProperty("phone_number_id")
        private String phoneNumberId;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhatsappContact {
        /** Nombre que el contacto puso en su perfil de WhatsApp. */
        @JsonProperty("profile")
        private WhatsappProfile profile;

        /** WhatsApp ID — equivalente al phone sin "+". */
        @JsonProperty("wa_id")
        private String waId;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhatsappProfile {
        @JsonProperty("name")
        private String name;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhatsappMessage {
        /**
         * Identificador único del mensaje (formato {@code wamid.xxx}).
         * Ahora lo logueamos para trazabilidad; a futuro habilita
         * idempotencia (deduplicar reintentos de Meta por wamid).
         */
        @JsonProperty("id")
        private String id;

        @JsonProperty("from")
        private String from;

        /** Unix timestamp en segundos (string). Útil para debugging temporal. */
        @JsonProperty("timestamp")
        private String timestamp;

        @JsonProperty("type")
        private String type;

        @JsonProperty("text")
        private WhatsappText text;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhatsappText {
        @JsonProperty("body")
        private String body;
    }
}

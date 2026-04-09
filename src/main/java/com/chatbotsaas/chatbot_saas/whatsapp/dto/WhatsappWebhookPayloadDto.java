package com.chatbotsaas.chatbot_saas.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class WhatsappWebhookPayloadDto {

    @JsonProperty("entry")
    private List<Entry> entry;

    @Getter
    public static class Entry{
        @JsonProperty("changes")
        private List<Change> changes;
    }

    @Getter
    public static class Change{
        @JsonProperty("value")
        private ChangeValue value;
    }

    @Getter
    public static class ChangeValue{
        @JsonProperty("messages")
        private List<WhatsappMessage> messages;

        @JsonProperty("metadata")
        private WhatsappMetadata metadata;
    }

    @Getter
    public static class WhatsappMessage {
        @JsonProperty("from")
        private String from;

        @JsonProperty("text")
        private WhatsappText text;

        @JsonProperty("type")
        private String type;
    }

    @Getter
    public static class WhatsappText {
        @JsonProperty("body")
        private String body;
    }

    @Getter
    public static class WhatsappMetadata {
        @JsonProperty("phone_number_id")
        private String phoneNumberId;
    }
}

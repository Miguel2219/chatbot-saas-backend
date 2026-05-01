package com.chatbotsaas.chatbot_saas.whatsapp.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Verifica firmas HMAC-SHA256 de webhooks de Meta WhatsApp Cloud API.
 *
 * <p>Meta firma cada POST del webhook con el header
 * {@code X-Hub-Signature-256: sha256=<hex>}. El valor {@code <hex>} es
 * {@code HMAC-SHA256(app_secret, raw_body_bytes)} en lowercase hex.
 *
 * <p>El App Secret se obtiene en Meta Developers Dashboard → Tu App →
 * Settings → Basic → "App Secret". Es distinto del Permanent Access
 * Token (que vive por bot en {@code whatsapp_configs.api_key}).
 *
 * <p>Comparación constant-time vía {@link MessageDigest#isEqual(byte[], byte[])}
 * — evita timing attacks que podrían filtrar la firma esperada.
 */
@Component
public class MetaSignatureVerifier {

    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String appSecret;

    public MetaSignatureVerifier(@Value("${app.whatsapp.app-secret}") String appSecret) {
        this.appSecret = appSecret;
    }

    /**
     * Devuelve {@code true} sólo si {@code headerValue} es exactamente
     * {@code "sha256=" + HMAC-SHA256-hex(appSecret, rawBody)}.
     * Cualquier null, prefijo ausente, o desigualdad → false.
     */
    public boolean verify(byte[] rawBody, String headerValue) {
        if (rawBody == null || headerValue == null || !headerValue.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }
        String expectedHex = computeHmacSha256Hex(rawBody);
        String receivedHex = headerValue.substring(SIGNATURE_PREFIX.length());
        return MessageDigest.isEqual(
                expectedHex.getBytes(StandardCharsets.US_ASCII),
                receivedHex.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private String computeHmacSha256Hex(byte[] payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    appSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(payload);
            return toHex(digest);
        } catch (Exception e) {
            // Fail-closed: cualquier error de crypto invalida la firma.
            throw new IllegalStateException("Failed to compute HMAC-SHA256", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

package com.chatbotsaas.chatbot_saas.conversation.scheduler;

import com.chatbotsaas.chatbot_saas.conversation.enums.ConversationStatus;
import com.chatbotsaas.chatbot_saas.conversation.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Job horario que resetea conversaciones WhatsApp atascadas en
 * {@link ConversationStatus#PENDING_HUMAN} hace más de
 * {@link #STUCK_HOURS} horas, devolviéndolas a
 * {@link ConversationStatus#BOT_ACTIVE} para que el bot vuelva a
 * responder.
 *
 * <p>Cron: {@code 0 0 * * * *} en zona América/Bogotá (cada hora a la
 * hora exacta). Espaciado respecto a otros jobs de mantenimiento que
 * corren a horarios fijos (00:05, 00:15, 00:30) — éste corre cada hora
 * en :00 y la convergencia con esos jobs ocurre sólo a las 00:00.
 *
 * <p>El umbral de 12 horas está hardcoded por simplicidad. Si se
 * requiere ajustarlo por cliente/canal, se puede externalizar a
 * {@code application.yml} en el futuro (ver PENDIENTES_POST_LAUNCH.md).
 */
@Component
@RequiredArgsConstructor
public class ConversationResetJob {

    private static final Logger log = LoggerFactory.getLogger(ConversationResetJob.class);
    private static final int STUCK_HOURS = 12;

    private final ConversationRepository conversationRepository;

    @Scheduled(cron = "0 0 * * * *", zone = "America/Bogota")
    @Transactional
    public void resetStuckConversations() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(STUCK_HOURS);
        int reset = conversationRepository.resetStatusForStuckSince(
                ConversationStatus.PENDING_HUMAN,
                ConversationStatus.BOT_ACTIVE,
                cutoff);
        log.info("ConversationResetJob: reset {} stuck PENDING_HUMAN conversations (older than {} hours, cutoff={})",
                reset, STUCK_HOURS, cutoff);
    }
}

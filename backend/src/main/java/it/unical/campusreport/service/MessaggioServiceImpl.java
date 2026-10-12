package it.unical.campusreport.service;

import it.unical.campusreport.dto.ChatResponse;
import it.unical.campusreport.dto.MessaggioRequest;
import it.unical.campusreport.dto.MessaggioResponse;
import it.unical.campusreport.dto.UserResponse;
import it.unical.campusreport.entity.Messaggio;
import it.unical.campusreport.entity.Ticket;
import it.unical.campusreport.entity.User;
import it.unical.campusreport.exception.ChatNonDisponibileException;
import it.unical.campusreport.exception.TicketNotFoundException;
import it.unical.campusreport.exception.UnauthorizedTicketAccessException;
import it.unical.campusreport.repository.MessaggioRepository;
import it.unical.campusreport.repository.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementazione di {@link MessaggioService}.
 */
@Service
@Slf4j
public class MessaggioServiceImpl implements MessaggioService {

    private final MessaggioRepository messaggioRepository;
    private final TicketRepository ticketRepository;
    private final EmailService emailService;

    public MessaggioServiceImpl(MessaggioRepository messaggioRepository,
                                TicketRepository ticketRepository,
                                EmailService emailService) {
        this.messaggioRepository = messaggioRepository;
        this.ticketRepository = ticketRepository;
        this.emailService = emailService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ChatResponse getChat(UUID ticketId, User utente) {
        Ticket ticket = caricaTicketChat(ticketId, utente);

        List<Messaggio> messaggi = messaggioRepository.findByTicketOrderByCreatedAtAsc(ticket);

        // Il conteggio va calcolato prima di marcare i messaggi come letti
        List<Messaggio> nonLetti = messaggi.stream()
                .filter(m -> !m.isLetto() && !isStessoUtente(m.getMittente(), utente))
                .toList();
        long numeroNonLetti = nonLetti.size();

        if (!nonLetti.isEmpty()) {
            nonLetti.forEach(m -> m.setLetto(true));
            messaggioRepository.saveAll(nonLetti);
            log.debug("Marcati come letti {} messaggi del ticket {} per {}",
                    numeroNonLetti, ticketId, utente.getEmail());
        }

        return ChatResponse.builder()
                .ticketId(ticket.getId())
                .messaggi(messaggi.stream().map(m -> toMessaggioResponse(m, utente)).toList())
                .nonLetti(numeroNonLetti)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public MessaggioResponse inviaMessaggio(UUID ticketId, MessaggioRequest request, User mittente) {
        Ticket ticket = caricaTicketChat(ticketId, mittente);

        Messaggio messaggio = messaggioRepository.save(Messaggio.builder()
                .ticket(ticket)
                .mittente(mittente)
                .testo(request.getTesto().trim())
                .letto(false)
                .build());

        User destinatario = isStessoUtente(ticket.getSegnalante(), mittente)
                ? ticket.getTecnico()
                : ticket.getSegnalante();

        log.info("Messaggio inviato da {} a {} sul ticket {}",
                mittente.getEmail(), destinatario.getEmail(), ticketId);

        emailService.notificaNuovoMessaggio(messaggio, destinatario);

        return toMessaggioResponse(messaggio, mittente);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public long contaNonLetti(UUID ticketId, User utente) {
        Ticket ticket = caricaTicketChat(ticketId, utente);
        return messaggioRepository.countByTicketAndLettoFalseAndMittenteNot(ticket, utente);
    }

    // ─── Helper privati ─────────────────────────────────────────────────────────

    /**
     * Carica il ticket e verifica che l'utente sia il segnalante o il tecnico
     * assegnato e che la chat sia disponibile (tecnico presente).
     */
    private Ticket caricaTicketChat(UUID ticketId, User utente) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket non trovato con id: " + ticketId));

        boolean isSegnalante = isStessoUtente(ticket.getSegnalante(), utente);
        boolean isTecnico = ticket.getTecnico() != null && isStessoUtente(ticket.getTecnico(), utente);
        if (!isSegnalante && !isTecnico) {
            throw new UnauthorizedTicketAccessException("Non sei autorizzato ad accedere alla chat di questo ticket");
        }

        if (ticket.getTecnico() == null) {
            throw new ChatNonDisponibileException(
                    "La chat è disponibile solo quando la segnalazione ha un tecnico assegnato");
        }

        return ticket;
    }

    private boolean isStessoUtente(User a, User b) {
        return a != null && b != null && a.getId().equals(b.getId());
    }

    private MessaggioResponse toMessaggioResponse(Messaggio messaggio, User utenteCorrente) {
        return MessaggioResponse.builder()
                .id(messaggio.getId())
                .ticketId(messaggio.getTicket().getId())
                .mittente(toUserResponse(messaggio.getMittente()))
                .testo(messaggio.getTesto())
                .createdAt(messaggio.getCreatedAt())
                .letto(messaggio.isLetto())
                .mio(isStessoUtente(messaggio.getMittente(), utenteCorrente))
                .build();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .nome(user.getNome())
                .cognome(user.getCognome())
                .email(user.getEmail())
                .ruolo(user.getRuolo())
                .build();
    }
}

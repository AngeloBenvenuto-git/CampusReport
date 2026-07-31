package it.unical.campusreport.service;

import it.unical.campusreport.entity.Ticket;
import it.unical.campusreport.entity.User;
import it.unical.campusreport.entity.enums.Stato;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Implementazione di {@link EmailService} basata su {@link JavaMailSender}
 * e {@link SimpleMailMessage} (email in solo testo).
 *
 * <p>Ogni metodo racchiude l'invio in un try-catch che logga eventuali
 * errori senza rilanciarli: una notifica non recapitata non deve
 * interrompere il flusso applicativo che l'ha generata.
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String from;
    private final String adminEmail;

    public EmailServiceImpl(JavaMailSender mailSender,
                             @Value("${app.email.from}") String from,
                             @Value("${app.email.admin-email}") String adminEmail) {
        this.mailSender = mailSender;
        this.from = from;
        this.adminEmail = adminEmail;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notificaTecnicoNuovaAssegnazione(Ticket ticket, User tecnico) {
        try {
            String corpo = """
                    Gentile %s,

                    Le è stata assegnata una nuova segnalazione.

                    Dettagli:
                    - Titolo: %s
                    - Categoria: %s
                    - Zona: %s
                    - Priorità: %s
                    - Descrizione: %s

                    Acceda al sistema per prendere in carico la segnalazione.

                    CampusReport - Sistema segnalazioni Unical"""
                    .formatted(
                            tecnico.getNome(),
                            ticket.getTitolo(),
                            ticket.getCategoria(),
                            ticket.getZona().getNome(),
                            ticket.getPriorita(),
                            ticket.getDescrizione());

            invia(tecnico.getEmail(),
                    "[CampusReport] Nuova segnalazione assegnata - " + ticket.getTitolo(),
                    corpo);

            log.info("Email di assegnazione inviata a {} per ticket {}", tecnico.getEmail(), ticket.getId());
        } catch (Exception e) {
            log.error("Errore nell'invio dell'email di assegnazione al tecnico {} per ticket {}",
                    tecnico.getEmail(), ticket.getId(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notificaUtenteStatoCambiato(Ticket ticket, Stato statoNuovo) {
        try {
            User segnalante = ticket.getSegnalante();

            String corpo = """
                    Gentile %s,

                    La sua segnalazione '%s' ha cambiato stato.

                    Stato attuale: %s
                    %s

                    CampusReport - Sistema segnalazioni Unical"""
                    .formatted(
                            segnalante.getNome(),
                            ticket.getTitolo(),
                            statoNuovo,
                            messaggioPerStato(statoNuovo));

            invia(segnalante.getEmail(), oggettoPerStato(statoNuovo), corpo);

            log.info("Email di cambio stato ({}) inviata a {} per ticket {}",
                    statoNuovo, segnalante.getEmail(), ticket.getId());
        } catch (Exception e) {
            log.error("Errore nell'invio dell'email di cambio stato per ticket {}", ticket.getId(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notificaAdminTicketInAttesa(Ticket ticket) {
        try {
            String corpo = """
                    Attenzione amministratore,

                    Un ticket non ha potuto essere assegnato automaticamente.

                    Dettagli:
                    - ID: %s
                    - Titolo: %s
                    - Categoria: %s
                    - Zona: %s
                    - Segnalante: %s

                    Intervenire manualmente per assegnare il ticket a un tecnico.

                    CampusReport - Sistema segnalazioni Unical"""
                    .formatted(
                            ticket.getId(),
                            ticket.getTitolo(),
                            ticket.getCategoria(),
                            ticket.getZona().getNome(),
                            ticket.getSegnalante().getEmail());

            invia(adminEmail, "[CampusReport] ATTENZIONE: Ticket in attesa di assegnazione", corpo);

            log.info("Email di notifica admin inviata per ticket {} in attesa", ticket.getId());
        } catch (Exception e) {
            log.error("Errore nell'invio dell'email di notifica admin per ticket {}", ticket.getId(), e);
        }
    }

    // ─── Helper privati ─────────────────────────────────────────────────────────

    private void invia(String to, String oggetto, String corpo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(oggetto);
        message.setText(corpo);
        mailSender.send(message);
    }

    private String oggettoPerStato(Stato stato) {
        return switch (stato) {
            case ASSEGNATA -> "[CampusReport] Segnalazione presa in carico";
            case IN_LAVORAZIONE -> "[CampusReport] Lavori in corso";
            case COMPLETATA -> "[CampusReport] Segnalazione risolta";
            case IN_ATTESA -> "[CampusReport] Segnalazione in attesa";
            case RIFIUTATA -> "[CampusReport] Segnalazione riassegnata";
            default -> "[CampusReport] Aggiornamento segnalazione";
        };
    }

    private String messaggioPerStato(Stato stato) {
        return switch (stato) {
            case ASSEGNATA -> "Un tecnico è stato assegnato alla sua segnalazione.";
            case IN_LAVORAZIONE -> "Il tecnico ha iniziato a lavorare al problema.";
            case COMPLETATA -> "Il problema è stato risolto. Grazie per la segnalazione.";
            case IN_ATTESA -> "Nessun tecnico disponibile al momento. Sarà contattato appena possibile.";
            case RIFIUTATA -> "La segnalazione è stata riassegnata a un altro tecnico.";
            default -> "";
        };
    }
}

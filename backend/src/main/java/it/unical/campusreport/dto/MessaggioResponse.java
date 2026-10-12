package it.unical.campusreport.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO di risposta per un singolo messaggio della chat di un ticket.
 */
@Data
@Builder
public class MessaggioResponse {
    private UUID id;
    private UUID ticketId;
    private UserResponse mittente;
    private String testo;
    private LocalDateTime createdAt;
    private boolean letto;
    /** True se il mittente è l'utente autenticato che effettua la richiesta. */
    private boolean mio;
}

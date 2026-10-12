package it.unical.campusreport.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * DTO di risposta con la conversazione completa di un ticket.
 */
@Data
@Builder
public class ChatResponse {
    private UUID ticketId;
    private List<MessaggioResponse> messaggi;
    /** Messaggi non letti dall'utente corrente prima di questa lettura. */
    private long nonLetti;
}

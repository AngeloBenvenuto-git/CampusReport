package it.unical.campusreport.service;

import it.unical.campusreport.dto.ChatResponse;
import it.unical.campusreport.dto.MessaggioRequest;
import it.unical.campusreport.dto.MessaggioResponse;
import it.unical.campusreport.entity.User;

import java.util.UUID;

/**
 * Servizio per la chat tra segnalante e tecnico assegnato a un ticket.
 */
public interface MessaggioService {

    /**
     * Restituisce la conversazione di un ticket e marca come letti i messaggi
     * ricevuti dall'utente corrente.
     *
     * @param ticketId identificativo del ticket
     * @param utente   utente richiedente (segnalante o tecnico assegnato)
     * @return la chat con i messaggi in ordine cronologico e il numero di non letti
     *         precedenti alla lettura
     * @throws it.unical.campusreport.exception.TicketNotFoundException           se il ticket non esiste
     * @throws it.unical.campusreport.exception.UnauthorizedTicketAccessException  se l'utente non partecipa alla chat
     * @throws it.unical.campusreport.exception.ChatNonDisponibileException        se il ticket non ha un tecnico assegnato
     */
    ChatResponse getChat(UUID ticketId, User utente);

    /**
     * Invia un nuovo messaggio nella chat di un ticket e notifica via email
     * l'altro partecipante.
     *
     * @param ticketId identificativo del ticket
     * @param request  testo del messaggio
     * @param mittente utente che invia il messaggio (segnalante o tecnico assegnato)
     * @return il messaggio salvato
     * @throws it.unical.campusreport.exception.TicketNotFoundException           se il ticket non esiste
     * @throws it.unical.campusreport.exception.UnauthorizedTicketAccessException  se l'utente non partecipa alla chat
     * @throws it.unical.campusreport.exception.ChatNonDisponibileException        se il ticket non ha un tecnico assegnato
     */
    MessaggioResponse inviaMessaggio(UUID ticketId, MessaggioRequest request, User mittente);

    /**
     * Conta i messaggi non letti dall'utente nella chat di un ticket, senza
     * marcarli come letti. Usato dal polling del frontend.
     *
     * @param ticketId identificativo del ticket
     * @param utente   utente richiedente (segnalante o tecnico assegnato)
     * @return numero di messaggi ricevuti e non ancora letti
     */
    long contaNonLetti(UUID ticketId, User utente);
}

package it.unical.campusreport.service;

import it.unical.campusreport.dto.AggiornamentoStatoRequest;
import it.unical.campusreport.dto.RifiutoRequest;
import it.unical.campusreport.dto.TicketRequest;
import it.unical.campusreport.dto.TicketResponse;
import it.unical.campusreport.entity.User;

import java.util.List;
import java.util.UUID;

/**
 * Servizio per la gestione del ciclo di vita dei ticket di segnalazione.
 */
public interface TicketService {

    /**
     * Crea un nuovo ticket e registra il cambio di stato iniziale (APERTA).
     *
     * @param request dati del ticket
     * @param utente  utente che crea la segnalazione
     * @return ticket creato
     */
    TicketResponse creaTicket(TicketRequest request, User utente);

    /**
     * Restituisce tutti i ticket dell'utente ordinati per data di creazione decrescente.
     *
     * @param utente utente segnalante
     * @return lista dei ticket dell'utente
     */
    List<TicketResponse> getTicketsByUser(User utente);

    /**
     * Restituisce un ticket per ID verificando che l'utente abbia i permessi di accesso.
     *
     * @param id     identificativo del ticket
     * @param utente utente richiedente
     * @return il ticket corrispondente
     * @throws it.unical.campusreport.exception.TicketNotFoundException          se il ticket non esiste
     * @throws it.unical.campusreport.exception.UnauthorizedTicketAccessException se l'utente non ha accesso
     */
    TicketResponse getTicketById(UUID id, User utente);

    /**
     * Restituisce i ticket ASSEGNATA o IN_LAVORAZIONE assegnati al tecnico,
     * ordinati per priorità decrescente e data di creazione crescente.
     *
     * @param tecnico il tecnico autenticato
     * @return lista dei ticket assegnati
     */
    List<TicketResponse> getTicketsAssegnati(User tecnico);

    /**
     * Aggiorna lo stato di un ticket. Transizioni valide:
     * ASSEGNATA → IN_LAVORAZIONE, IN_LAVORAZIONE → COMPLETATA.
     *
     * @param id      identificativo del ticket
     * @param request nuovo stato e nota opzionale
     * @param tecnico tecnico che effettua l'aggiornamento
     * @return ticket aggiornato
     * @throws it.unical.campusreport.exception.UnauthorizedTicketAccessException  se il ticket non è assegnato a questo tecnico
     * @throws it.unical.campusreport.exception.InvalidStatoTransitionException    se la transizione non è consentita
     */
    TicketResponse aggiornaStato(UUID id, AggiornamentoStatoRequest request, User tecnico);

    /**
     * Rifiuta un ticket con motivazione obbligatoria.
     *
     * <p>Con {@link it.unical.campusreport.entity.enums.TipoRifiuto#RIASSEGNA} il ticket
     * viene riassegnato automaticamente a un altro tecnico disponibile e viene restituito
     * il ticket aggiornato. Con {@link it.unical.campusreport.entity.enums.TipoRifiuto#ELIMINA}
     * il ticket viene eliminato definitivamente dal sistema e il metodo restituisce
     * {@code null}.
     *
     * @param id      identificativo del ticket
     * @param request tipo di rifiuto e motivazione
     * @param tecnico tecnico che rifiuta
     * @return ticket aggiornato con stato RIFIUTATA, oppure {@code null} se il ticket è
     *         stato eliminato definitivamente
     */
    TicketResponse rifiutaTicket(UUID id, RifiutoRequest request, User tecnico);
}

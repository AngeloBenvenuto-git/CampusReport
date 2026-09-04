package it.unical.campusreport.service;

import it.unical.campusreport.dto.AdminTicketFilter;
import it.unical.campusreport.dto.AssegnazioneManualRequest;
import it.unical.campusreport.dto.TicketResponse;
import it.unical.campusreport.entity.User;

import java.util.List;
import java.util.UUID;

/**
 * Servizio per la gestione dei ticket dal pannello amministratore:
 * ricerca filtrata, assegnazione manuale ed esportazione CSV.
 */
public interface AdminTicketService {

    /**
     * Restituisce i ticket di sistema filtrati e paginati, ordinati per priorità
     * operativa: IN_ATTESA prima, poi priorità ALTA, poi data di creazione decrescente.
     *
     * @param filter filtri opzionali e parametri di paginazione
     * @return pagina di ticket corrispondenti ai filtri
     */
    List<TicketResponse> getAllTickets(AdminTicketFilter filter);

    /**
     * Assegna manualmente un ticket a un tecnico specifico, sovrascrivendo
     * l'eventuale assegnazione automatica precedente.
     *
     * @param ticketId identificativo del ticket
     * @param request  identificativo del tecnico da assegnare
     * @param admin    amministratore che effettua l'assegnazione
     * @return ticket aggiornato
     * @throws it.unical.campusreport.exception.TicketNotFoundException  se il ticket non esiste
     * @throws it.unical.campusreport.exception.TecnicoNotFoundException se il tecnico non esiste
     * @throws it.unical.campusreport.exception.TecnicoNonAttivoException se il tecnico non è attivo
     */
    TicketResponse assegnaManualmente(UUID ticketId, AssegnazioneManualRequest request, User admin);

    /**
     * Esporta tutti i ticket del sistema in formato CSV.
     *
     * @return contenuto del file CSV codificato UTF-8
     */
    byte[] exportCsv();
}

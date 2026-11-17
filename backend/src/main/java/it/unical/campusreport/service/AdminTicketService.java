package it.unical.campusreport.service;

import it.unical.campusreport.dto.AdminModificaTicketRequest;
import it.unical.campusreport.dto.AdminTicketFilter;
import it.unical.campusreport.dto.AssegnazioneManualRequest;
import it.unical.campusreport.dto.TicketResponse;
import it.unical.campusreport.entity.User;

import java.util.List;
import java.util.UUID;

/**
 * Servizio per la gestione dei ticket dal pannello amministratore:
 * ricerca filtrata, assegnazione manuale, modifica, eliminazione ed esportazione CSV.
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
     * Esporta tutti i ticket del sistema in formato Excel (.xlsx).
     *
     * @return contenuto del file Excel
     */
    byte[] exportExcel();

    /**
     * Modifica un ticket in qualsiasi stato. Il tecnico assegnato non viene
     * ricalcolato anche se cambia la categoria. Se l'urgenza è valorizzata,
     * la priorità viene ricalcolata (urgenza &gt;= 4 → ALTA, altrimenti NORMALE).
     * Registra un cambio di stato di tracciamento e, se il ticket ha un tecnico,
     * gli invia un'email con i valori precedenti.
     *
     * @param id      identificativo del ticket
     * @param request nuovi valori del ticket
     * @param admin   amministratore che effettua la modifica
     * @return ticket aggiornato
     * @throws it.unical.campusreport.exception.TicketNotFoundException se il ticket non esiste
     * @throws it.unical.campusreport.exception.ZonaNotFoundException   se la zona non esiste
     */
    TicketResponse modificaTicket(UUID id, AdminModificaTicketRequest request, User admin);

    /**
     * Elimina definitivamente un ticket con chat, storico e allegati (file compresi)
     * e avvisa via email il segnalante e l'eventuale tecnico assegnato.
     *
     * @param id identificativo del ticket
     * @throws it.unical.campusreport.exception.TicketNotFoundException se il ticket non esiste
     */
    void eliminaTicket(UUID id);
}

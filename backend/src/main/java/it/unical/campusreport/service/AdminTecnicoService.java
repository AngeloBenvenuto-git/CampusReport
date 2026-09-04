package it.unical.campusreport.service;

import it.unical.campusreport.dto.CambioStatoTecnicoRequest;
import it.unical.campusreport.dto.CreaTecnicoRequest;
import it.unical.campusreport.dto.ModificaTecnicoRequest;
import it.unical.campusreport.dto.TecnicoAdminResponse;

import java.util.List;
import java.util.UUID;

/**
 * Servizio per la gestione degli account tecnico da parte dell'amministratore:
 * creazione con invito via email, modifica, attivazione/disattivazione.
 */
public interface AdminTecnicoService {

    /**
     * Restituisce tutti i tecnici con il relativo carico attuale
     * (ticket in stato ASSEGNATA o IN_LAVORAZIONE).
     *
     * @return lista di tutti i tecnici
     */
    List<TecnicoAdminResponse> getAllTecnici();

    /**
     * Crea un nuovo account tecnico (inizialmente non attivo), genera un token
     * di attivazione valido 48 ore e invia l'email di invito.
     *
     * @param request dati del nuovo tecnico
     * @return tecnico creato
     * @throws it.unical.campusreport.exception.EmailAlreadyExistsException se l'email è già registrata
     */
    TecnicoAdminResponse creaTecnico(CreaTecnicoRequest request);

    /**
     * Modifica specializzazioni, zona e carico massimo di un tecnico esistente.
     *
     * @param id      identificativo del tecnico
     * @param request nuovi dati
     * @return tecnico aggiornato
     * @throws it.unical.campusreport.exception.TecnicoNotFoundException se il tecnico non esiste
     */
    TecnicoAdminResponse modificaTecnico(UUID id, ModificaTecnicoRequest request);

    /**
     * Attiva o disattiva un account tecnico.
     *
     * @param id      identificativo del tecnico
     * @param request nuovo stato di attivazione
     * @return tecnico aggiornato
     * @throws it.unical.campusreport.exception.TecnicoNotFoundException se il tecnico non esiste
     */
    TecnicoAdminResponse cambiaStato(UUID id, CambioStatoTecnicoRequest request);
}

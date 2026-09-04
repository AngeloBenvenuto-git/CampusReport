package it.unical.campusreport.service;

import it.unical.campusreport.dto.ConfigPesiRequest;
import it.unical.campusreport.dto.ConfigPesiResponse;

/**
 * Servizio per la lettura e l'aggiornamento a runtime dei pesi alpha e beta
 * dell'algoritmo di assegnazione automatica dei ticket.
 */
public interface AdminConfigService {

    /**
     * Restituisce i pesi correnti dell'algoritmo di assegnazione.
     *
     * @return pesi alpha e beta attuali
     */
    ConfigPesiResponse getPesi();

    /**
     * Aggiorna a runtime i pesi alpha e beta dell'algoritmo di assegnazione.
     *
     * @param request nuovi pesi (la somma deve essere 1.0)
     * @return pesi aggiornati
     * @throws it.unical.campusreport.exception.ConfigPesiInvalidiException se alpha + beta ≠ 1.0
     */
    ConfigPesiResponse aggiornaPesi(ConfigPesiRequest request);
}

package it.unical.campusreport.service;

import it.unical.campusreport.dto.AdminStatisticheResponse;

/**
 * Servizio per il calcolo delle statistiche globali del sistema
 * mostrate nella dashboard dell'amministratore.
 */
public interface AdminStatisticheService {

    /**
     * Calcola le statistiche aggregate: ticket attivi, in attesa, tecnici attivi,
     * tempo medio di risoluzione, distribuzione per stato/categoria, andamento
     * settimanale e performance dei tecnici.
     *
     * @return statistiche correnti del sistema
     */
    AdminStatisticheResponse getStatistiche();
}

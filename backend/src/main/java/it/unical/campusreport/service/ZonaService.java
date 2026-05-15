package it.unical.campusreport.service;

import it.unical.campusreport.dto.ZonaResponse;

import java.util.List;
import java.util.UUID;

/**
 * Servizio per la gestione delle zone del campus.
 */
public interface ZonaService {

    /**
     * Restituisce tutte le zone disponibili.
     *
     * @return lista di tutte le zone
     */
    List<ZonaResponse> getAllZone();

    /**
     * Restituisce una zona per ID.
     *
     * @param id identificativo della zona
     * @return la zona corrispondente
     * @throws it.unical.campusreport.exception.ZonaNotFoundException se la zona non esiste
     */
    ZonaResponse getZonaById(UUID id);
}

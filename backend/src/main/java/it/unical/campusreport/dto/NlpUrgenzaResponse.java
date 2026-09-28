package it.unical.campusreport.dto;

import lombok.Data;

/**
 * Risposta del microservizio NLP per la stima dell'urgenza di una segnalazione.
 */
@Data
public class NlpUrgenzaResponse {

    /** Livello di urgenza stimato, da 1 (minima) a 5 (massima). */
    private int urgenza;
    private float confidenza;
    /** Descrizione testuale del livello (es. "problema grave"). */
    private String descrizione;
}

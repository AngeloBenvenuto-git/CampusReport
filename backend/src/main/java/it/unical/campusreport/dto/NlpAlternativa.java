package it.unical.campusreport.dto;

import lombok.Data;

/**
 * Categoria alternativa restituita dal microservizio NLP con il relativo punteggio di confidenza.
 */
@Data
public class NlpAlternativa {

    private String categoria;
    private float score;
}

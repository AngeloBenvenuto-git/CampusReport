package it.unical.campusreport.dto;

import lombok.Data;

import java.util.List;

/**
 * Risposta del microservizio NLP per la classificazione di un testo.
 */
@Data
public class NlpResponse {

    private String categoria;
    private float confidenza;
    private List<NlpAlternativa> alternative;
}

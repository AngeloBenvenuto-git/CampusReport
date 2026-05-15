package it.unical.campusreport.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO di richiesta per il rifiuto di un ticket da parte del tecnico.
 */
@Data
public class RifiutoRequest {

    @NotBlank(message = "La motivazione del rifiuto è obbligatoria")
    private String motivazione;
}

package it.unical.campusreport.dto;

import it.unical.campusreport.entity.enums.Stato;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO di richiesta per l'aggiornamento dello stato di un ticket da parte del tecnico.
 */
@Data
public class AggiornamentoStatoRequest {

    @NotNull(message = "Il nuovo stato è obbligatorio")
    private Stato statoNuovo;

    private String nota;
}

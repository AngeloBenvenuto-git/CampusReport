package it.unical.campusreport.dto;

import lombok.Data;

/**
 * DTO di richiesta per attivare o disattivare un account tecnico.
 */
@Data
public class CambioStatoTecnicoRequest {
    private boolean attivo;
}

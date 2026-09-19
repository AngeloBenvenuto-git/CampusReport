package it.unical.campusreport.dto;

import it.unical.campusreport.entity.enums.TipoRifiuto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO di richiesta per il rifiuto di un ticket da parte del tecnico.
 */
@Data
public class RifiutoRequest {

    @NotNull(message = "Il tipo di rifiuto è obbligatorio")
    private TipoRifiuto tipoRifiuto;

    @NotBlank(message = "La motivazione del rifiuto è obbligatoria")
    @Size(min = 10, message = "La motivazione deve contenere almeno 10 caratteri")
    private String motivazione;
}

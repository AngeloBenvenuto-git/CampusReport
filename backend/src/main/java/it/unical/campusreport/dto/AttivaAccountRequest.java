package it.unical.campusreport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO per la richiesta di attivazione di un account tecnico tramite il token ricevuto via email.
 */
@Data
public class AttivaAccountRequest {

    @NotBlank(message = "Il token è obbligatorio")
    private String token;

    @NotBlank(message = "La password è obbligatoria")
    @Size(min = 8, message = "La password deve contenere almeno 8 caratteri")
    private String password;

    @NotBlank(message = "La conferma password è obbligatoria")
    private String confermaPassword;
}

package it.unical.campusreport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO di richiesta per l'invio di un messaggio nella chat di un ticket.
 */
@Data
public class MessaggioRequest {

    @NotBlank(message = "Il testo del messaggio è obbligatorio")
    @Size(max = 2000, message = "Il messaggio non può superare 2000 caratteri")
    private String testo;
}

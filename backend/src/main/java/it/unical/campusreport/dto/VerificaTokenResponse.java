package it.unical.campusreport.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO di risposta per la verifica preventiva di un token di attivazione.
 * Se il token è valido contiene i dati dell'utente, altrimenti il motivo dell'invalidità.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerificaTokenResponse {

    private boolean valido;
    private String motivo;
    private String nome;
    private String cognome;
    private String email;
}

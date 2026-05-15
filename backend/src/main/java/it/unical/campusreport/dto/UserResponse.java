package it.unical.campusreport.dto;

import it.unical.campusreport.entity.enums.Ruolo;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * DTO di risposta con i dati essenziali di un utente.
 */
@Data
@Builder
public class UserResponse {
    private UUID id;
    private String nome;
    private String cognome;
    private String email;
    private Ruolo ruolo;
}

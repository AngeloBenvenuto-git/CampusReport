package it.unical.campusreport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO di risposta per le operazioni di autenticazione (login e register).
 * Contiene il token JWT e le informazioni dell'utente autenticato.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;

    @Builder.Default
    private String tipo = "Bearer";

    private UUID id;
    private String email;
    private String ruolo;
    private String nome;
    private String cognome;
}

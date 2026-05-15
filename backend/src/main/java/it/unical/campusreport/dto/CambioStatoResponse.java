package it.unical.campusreport.dto;

import it.unical.campusreport.entity.enums.Stato;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO di risposta per un singolo cambio di stato di un ticket.
 */
@Data
@Builder
public class CambioStatoResponse {
    private UUID id;
    private Stato statoPrecedente;
    private Stato statoNuovo;
    private UserResponse utente;
    private String nota;
    private LocalDateTime timestamp;
}

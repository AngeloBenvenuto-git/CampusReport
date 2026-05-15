package it.unical.campusreport.dto;

import it.unical.campusreport.entity.enums.Categoria;
import it.unical.campusreport.entity.enums.Priorita;
import it.unical.campusreport.entity.enums.Stato;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO di risposta completo per un ticket di segnalazione.
 */
@Data
@Builder
public class TicketResponse {
    private UUID id;
    private String titolo;
    private String descrizione;
    private Categoria categoria;
    private Stato stato;
    private Priorita priorita;
    private ZonaResponse zona;
    private UserResponse segnalante;
    private UserResponse tecnico;
    private Float categoriaConfidenza;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CambioStatoResponse> storico;
}

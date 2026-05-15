package it.unical.campusreport.dto;

import it.unical.campusreport.entity.enums.Categoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/**
 * DTO di richiesta per la creazione di un nuovo ticket.
 */
@Data
public class TicketRequest {

    @NotNull(message = "La zona è obbligatoria")
    private UUID zonaId;

    @NotBlank(message = "Il titolo è obbligatorio")
    @Size(max = 200, message = "Il titolo non può superare 200 caratteri")
    private String titolo;

    @NotBlank(message = "La descrizione è obbligatoria")
    @Size(max = 2000, message = "La descrizione non può superare 2000 caratteri")
    private String descrizione;

    @NotNull(message = "La categoria è obbligatoria")
    private Categoria categoria;
}

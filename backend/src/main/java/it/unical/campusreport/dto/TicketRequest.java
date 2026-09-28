package it.unical.campusreport.dto;

import it.unical.campusreport.entity.enums.Categoria;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    private String cubo;

    private String piano;

    @NotBlank(message = "Il titolo è obbligatorio")
    @Size(max = 200, message = "Il titolo non può superare 200 caratteri")
    private String titolo;

    @NotBlank(message = "La descrizione è obbligatoria")
    @Size(max = 2000, message = "La descrizione non può superare 2000 caratteri")
    private String descrizione;

    @NotNull(message = "La categoria è obbligatoria")
    private Categoria categoria;

    /** Urgenza 1-5 indicata dall'utente; se null si usa la stima del microservizio NLP. */
    @Min(value = 1, message = "L'urgenza minima è 1")
    @Max(value = 5, message = "L'urgenza massima è 5")
    private Integer urgenza;
}

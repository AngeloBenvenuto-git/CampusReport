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
 * DTO di richiesta per la modifica di un ticket da parte dell'amministratore.
 *
 * <p>A differenza di {@link ModificaTicketRequest}, l'admin può cambiare anche
 * la zona e l'urgenza. Cubo, piano e urgenza sono opzionali: se l'urgenza è
 * null il valore attuale (e la priorità derivata) restano invariati.
 */
@Data
public class AdminModificaTicketRequest {

    @NotBlank(message = "Il titolo è obbligatorio")
    @Size(max = 200, message = "Il titolo non può superare 200 caratteri")
    private String titolo;

    @NotBlank(message = "La descrizione è obbligatoria")
    @Size(max = 2000, message = "La descrizione non può superare 2000 caratteri")
    private String descrizione;

    @NotNull(message = "La categoria è obbligatoria")
    private Categoria categoria;

    @NotNull(message = "La zona è obbligatoria")
    private UUID zonaId;

    private String cubo;

    private String piano;

    @Min(value = 1, message = "L'urgenza minima è 1")
    @Max(value = 5, message = "L'urgenza massima è 5")
    private Integer urgenza;
}

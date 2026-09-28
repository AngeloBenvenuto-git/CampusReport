package it.unical.campusreport.dto;

import it.unical.campusreport.entity.enums.Categoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO di richiesta per la modifica di un ticket esistente da parte del segnalante.
 *
 * <p>La zona non è modificabile: per cambiare zona l'utente deve creare una nuova
 * segnalazione. Cubo e piano restano opzionali come in fase di creazione.
 */
@Data
public class ModificaTicketRequest {

    @NotBlank(message = "Il titolo è obbligatorio")
    @Size(max = 200, message = "Il titolo non può superare 200 caratteri")
    private String titolo;

    @NotBlank(message = "La descrizione è obbligatoria")
    @Size(max = 2000, message = "La descrizione non può superare 2000 caratteri")
    private String descrizione;

    @NotNull(message = "La categoria è obbligatoria")
    private Categoria categoria;

    private String cubo;

    private String piano;
}

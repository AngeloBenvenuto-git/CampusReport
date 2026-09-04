package it.unical.campusreport.dto;

import it.unical.campusreport.entity.enums.Categoria;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * DTO di richiesta per la creazione di un nuovo tecnico da parte dell'admin.
 */
@Data
public class CreaTecnicoRequest {

    @NotBlank(message = "Il nome è obbligatorio")
    private String nome;

    @NotBlank(message = "Il cognome è obbligatorio")
    private String cognome;

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Formato email non valido")
    private String email;

    @NotEmpty(message = "Almeno una specializzazione è obbligatoria")
    private List<Categoria> specializzazioni;

    @NotBlank(message = "La zona è obbligatoria")
    private String zona;

    private int caricoMassimo = 10;
}

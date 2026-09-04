package it.unical.campusreport.dto;

import it.unical.campusreport.entity.enums.Categoria;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * DTO di richiesta per la modifica di specializzazioni, zona e carico massimo di un tecnico.
 */
@Data
public class ModificaTecnicoRequest {

    @NotEmpty(message = "Almeno una specializzazione è obbligatoria")
    private List<Categoria> specializzazioni;

    @NotBlank(message = "La zona è obbligatoria")
    private String zona;

    @Min(value = 1, message = "Il carico massimo deve essere almeno 1")
    private int caricoMassimo;
}

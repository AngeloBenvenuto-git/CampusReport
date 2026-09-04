package it.unical.campusreport.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * DTO di richiesta per l'assegnazione manuale di un ticket a un tecnico da parte dell'admin.
 */
@Data
public class AssegnazioneManualRequest {

    @NotNull(message = "L'identificativo del tecnico è obbligatorio")
    private UUID tecnicoId;
}

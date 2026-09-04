package it.unical.campusreport.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

/**
 * DTO di richiesta per l'aggiornamento dei pesi alpha e beta dell'algoritmo di assegnazione.
 * La somma di alpha e beta deve essere uguale a 1.0 (verificato lato service).
 */
@Data
public class ConfigPesiRequest {

    @DecimalMin(value = "0.0", message = "alpha deve essere >= 0")
    @DecimalMax(value = "1.0", message = "alpha deve essere <= 1")
    private double alpha;

    @DecimalMin(value = "0.0", message = "beta deve essere >= 0")
    @DecimalMax(value = "1.0", message = "beta deve essere <= 1")
    private double beta;
}

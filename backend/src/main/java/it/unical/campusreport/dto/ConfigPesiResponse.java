package it.unical.campusreport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO di risposta con i pesi correnti dell'algoritmo di assegnazione automatica.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigPesiResponse {
    private double alpha;
    private double beta;
}

package it.unical.campusreport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Riepilogo delle segnalazioni gestite da un tecnico, usato nella dashboard admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TecnicoPerformance {
    private String nome;
    private Long completate;
    private Long inLavorazione;
}

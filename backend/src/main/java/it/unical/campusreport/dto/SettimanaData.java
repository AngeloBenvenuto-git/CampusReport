package it.unical.campusreport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Conteggio dei ticket creati in una singola settimana, usato per i grafici
 * di andamento temporale della dashboard admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettimanaData {
    private String settimana;
    private Long count;
}

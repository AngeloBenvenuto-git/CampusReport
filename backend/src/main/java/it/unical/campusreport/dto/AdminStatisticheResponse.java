package it.unical.campusreport.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DTO di risposta con le statistiche globali del sistema per la dashboard admin.
 */
@Data
@Builder
public class AdminStatisticheResponse {
    private long totaleTicketAttivi;
    private long ticketInAttesa;
    private long tecniciAttivi;
    private double tempoMedioRisoluzioneOre;
    private Map<String, Long> ticketPerStato;
    private Map<String, Long> ticketPerCategoria;
    private List<SettimanaData> ticketPerSettimana;
    private List<TecnicoPerformance> performanceTecnici;
}

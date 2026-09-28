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
    /** Numero di segnalazioni per livello di urgenza (chiavi 1-5). */
    private Map<Integer, Long> distribuzioneUrgenza;
    private List<SettimanaData> ticketPerSettimana;
    private List<TecnicoPerformance> performanceTecnici;
}

package it.unical.campusreport.service;

import it.unical.campusreport.dto.AdminStatisticheResponse;
import it.unical.campusreport.dto.SettimanaData;
import it.unical.campusreport.dto.TecnicoPerformance;
import it.unical.campusreport.entity.Tecnico;
import it.unical.campusreport.entity.Ticket;
import it.unical.campusreport.entity.enums.Categoria;
import it.unical.campusreport.entity.enums.Ruolo;
import it.unical.campusreport.entity.enums.Stato;
import it.unical.campusreport.repository.TecnicoRepository;
import it.unical.campusreport.repository.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implementazione di {@link AdminStatisticheService}.
 */
@Service
@Slf4j
public class AdminStatisticheServiceImpl implements AdminStatisticheService {

    private static final int SETTIMANE_STORICO = 8;
    private static final DateTimeFormatter ETICHETTA_SETTIMANA = DateTimeFormatter.ofPattern("dd/MM");

    private final TicketRepository ticketRepository;
    private final TecnicoRepository tecnicoRepository;

    public AdminStatisticheServiceImpl(TicketRepository ticketRepository, TecnicoRepository tecnicoRepository) {
        this.ticketRepository = ticketRepository;
        this.tecnicoRepository = tecnicoRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public AdminStatisticheResponse getStatistiche() {
        log.debug("Calcolo statistiche globali per il pannello admin");

        Map<String, Long> ticketPerStato = calcolaTicketPerStato();
        Map<String, Long> ticketPerCategoria = calcolaTicketPerCategoria();
        long ticketInAttesa = ticketPerStato.getOrDefault(Stato.IN_ATTESA.name(), 0L);
        long totaleTicketAttivi = ticketPerStato.entrySet().stream()
                .filter(e -> !e.getKey().equals(Stato.COMPLETATA.name()) && !e.getKey().equals(Stato.RIFIUTATA.name()))
                .mapToLong(Map.Entry::getValue)
                .sum();

        List<Tecnico> tecniciAttivi = tecnicoRepository.findByRuoloAndAttivo(Ruolo.TECNICO, true);

        return AdminStatisticheResponse.builder()
                .totaleTicketAttivi(totaleTicketAttivi)
                .ticketInAttesa(ticketInAttesa)
                .tecniciAttivi(tecniciAttivi.size())
                .tempoMedioRisoluzioneOre(calcolaTempoMedioRisoluzioneOre())
                .ticketPerStato(ticketPerStato)
                .ticketPerCategoria(ticketPerCategoria)
                .ticketPerSettimana(calcolaTicketPerSettimana())
                .performanceTecnici(calcolaPerformanceTecnici(tecniciAttivi))
                .build();
    }

    // ─── Helper privati ─────────────────────────────────────────────────────────

    private Map<String, Long> calcolaTicketPerStato() {
        Map<String, Long> risultato = new LinkedHashMap<>();
        for (Stato stato : Stato.values()) {
            risultato.put(stato.name(), ticketRepository.countByStato(stato));
        }
        return risultato;
    }

    private Map<String, Long> calcolaTicketPerCategoria() {
        Map<String, Long> risultato = new LinkedHashMap<>();
        for (Categoria categoria : Categoria.values()) {
            risultato.put(categoria.name(), ticketRepository.countByCategoria(categoria));
        }
        return risultato;
    }

    private double calcolaTempoMedioRisoluzioneOre() {
        List<Ticket> completati = ticketRepository.findByStato(Stato.COMPLETATA);
        if (completati.isEmpty()) {
            return 0.0;
        }
        return completati.stream()
                .mapToDouble(t -> Duration.between(t.getCreatedAt(), t.getUpdatedAt()).toMinutes() / 60.0)
                .average()
                .orElse(0.0);
    }

    private List<SettimanaData> calcolaTicketPerSettimana() {
        List<Ticket> tutti = ticketRepository.findAll();
        LocalDate oggi = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.ITALY);

        List<SettimanaData> risultato = new ArrayList<>();
        for (int i = SETTIMANE_STORICO - 1; i >= 0; i--) {
            LocalDate inizioSettimana = oggi.minusWeeks(i).with(weekFields.dayOfWeek(), 1);
            LocalDate fineSettimana = inizioSettimana.plusDays(6);

            long count = tutti.stream()
                    .map(t -> t.getCreatedAt().toLocalDate())
                    .filter(d -> !d.isBefore(inizioSettimana) && !d.isAfter(fineSettimana))
                    .count();

            risultato.add(SettimanaData.builder()
                    .settimana(inizioSettimana.format(ETICHETTA_SETTIMANA))
                    .count(count)
                    .build());
        }
        return risultato;
    }

    private List<TecnicoPerformance> calcolaPerformanceTecnici(List<Tecnico> tecniciAttivi) {
        List<TecnicoPerformance> risultato = new ArrayList<>();
        for (Tecnico tecnico : tecniciAttivi) {
            long completate = ticketRepository.countByStatoAndTecnico(Stato.COMPLETATA, tecnico);
            long inLavorazione = ticketRepository.countByStatoAndTecnico(Stato.IN_LAVORAZIONE, tecnico);
            risultato.add(TecnicoPerformance.builder()
                    .nome(tecnico.getNome() + " " + tecnico.getCognome())
                    .completate(completate)
                    .inLavorazione(inLavorazione)
                    .build());
        }
        return risultato;
    }
}

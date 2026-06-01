package it.unical.campusreport.service;

import it.unical.campusreport.config.AssegnazioneConfig;
import it.unical.campusreport.entity.CambioStato;
import it.unical.campusreport.entity.Tecnico;
import it.unical.campusreport.entity.Ticket;
import it.unical.campusreport.entity.enums.Stato;
import it.unical.campusreport.repository.CambioStatoRepository;
import it.unical.campusreport.repository.TecnicoRepository;
import it.unical.campusreport.repository.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementazione dell'algoritmo di assegnazione automatica.
 *
 * <p>Formula: {@code score = α·match_specializzazione + β·(1 − carico_attuale)}
 *
 * <p><b>Gestione concorrenza:</b> {@link #trovaMiglioreTecnico} usa
 * {@code PESSIMISTIC_WRITE} per bloccare le righe dei tecnici candidati
 * fino al commit, prevenendo la race condition in cui due segnalazioni
 * simultanee leggono il carico di un tecnico (es. 9/10) e lo assegnano
 * entrambe, portando il carico effettivo oltre il limite.
 */
@Service
@Slf4j
public class AssegnazioneServiceImpl implements AssegnazioneService {

    private final TecnicoRepository tecnicoRepository;
    private final TicketRepository ticketRepository;
    private final CambioStatoRepository cambioStatoRepository;
    private final AssegnazioneConfig config;

    public AssegnazioneServiceImpl(TecnicoRepository tecnicoRepository,
                                   TicketRepository ticketRepository,
                                   CambioStatoRepository cambioStatoRepository,
                                   AssegnazioneConfig config) {
        this.tecnicoRepository = tecnicoRepository;
        this.ticketRepository = ticketRepository;
        this.cambioStatoRepository = cambioStatoRepository;
        this.config = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void assegna(Ticket ticket) {
        Stato statoPrecedente = ticket.getStato();
        Optional<Tecnico> migliore = trovaMiglioreTecnico(ticket, null);

        if (migliore.isPresent()) {
            Tecnico tecnico = migliore.get();
            double score = calcolaScore(tecnico, ticket);
            ticket.setTecnico(tecnico);
            ticket.setStato(Stato.ASSEGNATA);

            cambioStatoRepository.save(CambioStato.builder()
                    .ticket(ticket)
                    .statoPrecedente(statoPrecedente)
                    .statoNuovo(Stato.ASSEGNATA)
                    .utente(tecnico)
                    .build());

            log.info("Ticket {} assegnato al tecnico {} (score: {})",
                    ticket.getId(), tecnico.getEmail(), score);
        } else {
            ticket.setStato(Stato.IN_ATTESA);

            cambioStatoRepository.save(CambioStato.builder()
                    .ticket(ticket)
                    .statoPrecedente(statoPrecedente)
                    .statoNuovo(Stato.IN_ATTESA)
                    .utente(null)
                    .build());

            log.warn("Nessun tecnico disponibile per ticket {}, categoria {}",
                    ticket.getId(), ticket.getCategoria());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void riassegna(Ticket ticket, UUID tecnicoEscluso) {
        Optional<Tecnico> migliore = trovaMiglioreTecnico(ticket, tecnicoEscluso);

        if (migliore.isPresent()) {
            Tecnico nuovoTecnico = migliore.get();
            double score = calcolaScore(nuovoTecnico, ticket);
            ticket.setTecnico(nuovoTecnico);
            ticket.setStato(Stato.ASSEGNATA);

            cambioStatoRepository.save(CambioStato.builder()
                    .ticket(ticket)
                    .statoPrecedente(Stato.RIFIUTATA)
                    .statoNuovo(Stato.ASSEGNATA)
                    .utente(nuovoTecnico)
                    .build());

            log.info("Ticket {} riassegnato al tecnico {} (score: {})",
                    ticket.getId(), nuovoTecnico.getEmail(), score);
        } else {
            ticket.setTecnico(null);
            ticket.setStato(Stato.IN_ATTESA);

            cambioStatoRepository.save(CambioStato.builder()
                    .ticket(ticket)
                    .statoPrecedente(Stato.RIFIUTATA)
                    .statoNuovo(Stato.IN_ATTESA)
                    .utente(null)
                    .build());

            log.warn("Nessun tecnico disponibile per riassegnazione ticket {}", ticket.getId());
        }
    }

    // ─── Helper privati ─────────────────────────────────────────────────────────

    /**
     * Seleziona il tecnico con score massimo tra quelli con la specializzazione
     * richiesta e carico non saturo.
     *
     * <p>Usa {@code PESSIMISTIC_WRITE} (vedi Javadoc di classe) per prevenire
     * assegnazioni concorrenti oltre il limite di carico.
     */
    private Optional<Tecnico> trovaMiglioreTecnico(Ticket ticket, UUID tecnicoEscluso) {
        List<Tecnico> candidati = tecnicoRepository
                .findAttiviBySpecializzazioneWithLock(ticket.getCategoria());

        return candidati.stream()
                .filter(t -> tecnicoEscluso == null || !t.getId().equals(tecnicoEscluso))
                .filter(t -> {
                    long aperti = ticketRepository.countByTecnicoAndStatoIn(
                            t, List.of(Stato.ASSEGNATA, Stato.IN_LAVORAZIONE));
                    return (double) aperti / t.getCaricoMassimo() < 1.0;
                })
                .max(Comparator.comparingDouble(t -> calcolaScore(t, ticket)));
    }

    /**
     * Calcola lo score di un tecnico per un dato ticket.
     *
     * @return score in [0, 1], oppure -1.0 se il tecnico non ha la specializzazione
     */
    private double calcolaScore(Tecnico tecnico, Ticket ticket) {
        double matchSpec = tecnico.getSpecializzazioni().contains(ticket.getCategoria())
                ? 1.0 : 0.0;

        if (matchSpec == 0.0) {
            return -1.0;
        }

        long ticketAperti = ticketRepository.countByTecnicoAndStatoIn(
                tecnico, List.of(Stato.ASSEGNATA, Stato.IN_LAVORAZIONE));

        double caricoAttuale = Math.min(1.0,
                (double) ticketAperti / tecnico.getCaricoMassimo());

        return config.getAlpha() * matchSpec + config.getBeta() * (1.0 - caricoAttuale);
    }
}

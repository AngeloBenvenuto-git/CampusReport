package it.unical.campusreport.service;

import it.unical.campusreport.dto.*;
import it.unical.campusreport.entity.CambioStato;
import it.unical.campusreport.entity.Ticket;
import it.unical.campusreport.entity.User;
import it.unical.campusreport.entity.Zona;
import it.unical.campusreport.entity.enums.Priorita;
import it.unical.campusreport.entity.enums.Ruolo;
import it.unical.campusreport.entity.enums.Stato;
import it.unical.campusreport.exception.InvalidStatoTransitionException;
import it.unical.campusreport.exception.TicketNotFoundException;
import it.unical.campusreport.exception.UnauthorizedTicketAccessException;
import it.unical.campusreport.exception.ZonaNotFoundException;
import it.unical.campusreport.repository.CambioStatoRepository;
import it.unical.campusreport.repository.TicketRepository;
import it.unical.campusreport.repository.ZonaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementazione di {@link TicketService}.
 */
@Service
@Slf4j
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final ZonaRepository zonaRepository;
    private final CambioStatoRepository cambioStatoRepository;

    public TicketServiceImpl(TicketRepository ticketRepository,
                             ZonaRepository zonaRepository,
                             CambioStatoRepository cambioStatoRepository) {
        this.ticketRepository = ticketRepository;
        this.zonaRepository = zonaRepository;
        this.cambioStatoRepository = cambioStatoRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public TicketResponse creaTicket(TicketRequest request, User utente) {
        log.info("Creazione ticket per utente: {}", utente.getEmail());

        Zona zona = zonaRepository.findById(request.getZonaId())
                .orElseThrow(() -> new ZonaNotFoundException("Zona non trovata con id: " + request.getZonaId()));

        Priorita priorita = utente.getRuolo() == Ruolo.DOCENTE ? Priorita.ALTA : Priorita.NORMALE;

        Ticket ticket = Ticket.builder()
                .titolo(request.getTitolo())
                .descrizione(request.getDescrizione())
                .categoria(request.getCategoria())
                .stato(Stato.APERTA)
                .priorita(priorita)
                .zona(zona)
                .segnalante(utente)
                .tecnico(null)
                .build();

        ticket = ticketRepository.save(ticket);

        CambioStato cambioStato = CambioStato.builder()
                .ticket(ticket)
                .statoPrecedente(null)
                .statoNuovo(Stato.APERTA)
                .utente(utente)
                .build();

        cambioStatoRepository.save(cambioStato);

        log.info("Ticket {} creato con stato APERTA, priorità {}", ticket.getId(), priorita);
        return toTicketResponse(ticket, List.of(cambioStato));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsByUser(User utente) {
        log.debug("Recupero ticket per utente: {}", utente.getEmail());
        return ticketRepository.findBySegnalante(utente).stream()
                .sorted(Comparator.comparing(Ticket::getCreatedAt).reversed())
                .map(t -> toTicketResponse(t, cambioStatoRepository.findByTicketOrderByTimestampAsc(t)))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(UUID id, User utente) {
        log.debug("Recupero ticket {} per utente {}", id, utente.getEmail());

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket non trovato con id: " + id));

        Ruolo ruolo = utente.getRuolo();
        if (ruolo == Ruolo.STUDENTE || ruolo == Ruolo.DOCENTE) {
            if (!utente.equals(ticket.getSegnalante())) {
                throw new UnauthorizedTicketAccessException("Accesso non autorizzato al ticket " + id);
            }
        } else if (ruolo == Ruolo.TECNICO) {
            if (!utente.equals(ticket.getTecnico())) {
                throw new UnauthorizedTicketAccessException("Il ticket " + id + " non è assegnato a questo tecnico");
            }
        }
        // ADMIN: nessuna verifica aggiuntiva

        List<CambioStato> storico = cambioStatoRepository.findByTicketOrderByTimestampAsc(ticket);
        return toTicketResponse(ticket, storico);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsAssegnati(User tecnico) {
        log.debug("Recupero ticket assegnati al tecnico: {}", tecnico.getEmail());
        return ticketRepository.findByTecnicoAndStatoIn(tecnico, List.of(Stato.ASSEGNATA, Stato.IN_LAVORAZIONE))
                .stream()
                .sorted(Comparator.comparing(Ticket::getPriorita).reversed()
                        .thenComparing(Ticket::getCreatedAt))
                .map(t -> toTicketResponse(t, cambioStatoRepository.findByTicketOrderByTimestampAsc(t)))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public TicketResponse aggiornaStato(UUID id, AggiornamentoStatoRequest request, User tecnico) {
        log.info("Tecnico {} aggiorna stato ticket {}", tecnico.getEmail(), id);

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket non trovato con id: " + id));

        if (!tecnico.equals(ticket.getTecnico())) {
            throw new UnauthorizedTicketAccessException("Il ticket " + id + " non è assegnato a questo tecnico");
        }

        Stato statoCorrente = ticket.getStato();
        Stato statoNuovo = request.getStatoNuovo();
        validaTransizione(statoCorrente, statoNuovo);

        ticket.setStato(statoNuovo);
        ticket = ticketRepository.save(ticket);

        CambioStato cambioStato = CambioStato.builder()
                .ticket(ticket)
                .statoPrecedente(statoCorrente)
                .statoNuovo(statoNuovo)
                .utente(tecnico)
                .nota(request.getNota())
                .build();

        cambioStatoRepository.save(cambioStato);

        log.info("Ticket {} aggiornato a stato {}", id, statoNuovo);
        List<CambioStato> storico = cambioStatoRepository.findByTicketOrderByTimestampAsc(ticket);
        return toTicketResponse(ticket, storico);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public TicketResponse rifiutaTicket(UUID id, RifiutoRequest request, User tecnico) {
        log.info("Tecnico {} rifiuta ticket {}", tecnico.getEmail(), id);

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket non trovato con id: " + id));

        if (!tecnico.equals(ticket.getTecnico())) {
            throw new UnauthorizedTicketAccessException("Il ticket " + id + " non è assegnato a questo tecnico");
        }

        Stato statoPrecedente = ticket.getStato();
        ticket.setStato(Stato.RIFIUTATA);
        ticket.setTecnico(null);
        ticket = ticketRepository.save(ticket);

        CambioStato cambioStato = CambioStato.builder()
                .ticket(ticket)
                .statoPrecedente(statoPrecedente)
                .statoNuovo(Stato.RIFIUTATA)
                .utente(tecnico)
                .nota(request.getMotivazione())
                .build();

        cambioStatoRepository.save(cambioStato);

        log.info("Ticket {} rifiutato, tecnico rimosso", id);
        List<CambioStato> storico = cambioStatoRepository.findByTicketOrderByTimestampAsc(ticket);
        return toTicketResponse(ticket, storico);
    }

    // ─── Mapping helpers ────────────────────────────────────────────────────────

    private void validaTransizione(Stato corrente, Stato nuovo) {
        boolean valida = (corrente == Stato.ASSEGNATA && nuovo == Stato.IN_LAVORAZIONE)
                || (corrente == Stato.IN_LAVORAZIONE && nuovo == Stato.COMPLETATA);
        if (!valida) {
            throw new InvalidStatoTransitionException(
                    "Transizione non consentita: " + corrente + " → " + nuovo);
        }
    }

    private TicketResponse toTicketResponse(Ticket ticket, List<CambioStato> storico) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .titolo(ticket.getTitolo())
                .descrizione(ticket.getDescrizione())
                .categoria(ticket.getCategoria())
                .stato(ticket.getStato())
                .priorita(ticket.getPriorita())
                .zona(ticket.getZona() != null ? toZonaResponse(ticket.getZona()) : null)
                .segnalante(toUserResponse(ticket.getSegnalante()))
                .tecnico(ticket.getTecnico() != null ? toUserResponse(ticket.getTecnico()) : null)
                .categoriaConfidenza(ticket.getCategoriaConfidenza())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .storico(storico.stream().map(this::toCambioStatoResponse).collect(Collectors.toList()))
                .build();
    }

    private CambioStatoResponse toCambioStatoResponse(CambioStato cs) {
        return CambioStatoResponse.builder()
                .id(cs.getId())
                .statoPrecedente(cs.getStatoPrecedente())
                .statoNuovo(cs.getStatoNuovo())
                .utente(toUserResponse(cs.getUtente()))
                .nota(cs.getNota())
                .timestamp(cs.getTimestamp())
                .build();
    }

    private ZonaResponse toZonaResponse(Zona zona) {
        return ZonaResponse.builder()
                .id(zona.getId())
                .nome(zona.getNome())
                .descrizione(zona.getDescrizione())
                .geojson(zona.getGeojson())
                .colore(zona.getColore())
                .build();
    }

    private UserResponse toUserResponse(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .nome(user.getNome())
                .cognome(user.getCognome())
                .email(user.getEmail())
                .ruolo(user.getRuolo())
                .build();
    }
}

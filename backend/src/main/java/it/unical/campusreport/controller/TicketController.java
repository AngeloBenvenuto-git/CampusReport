package it.unical.campusreport.controller;

import it.unical.campusreport.dto.AggiornamentoStatoRequest;
import it.unical.campusreport.dto.RifiutoRequest;
import it.unical.campusreport.dto.TicketRequest;
import it.unical.campusreport.dto.TicketResponse;
import it.unical.campusreport.entity.User;
import it.unical.campusreport.exception.UserNotFoundException;
import it.unical.campusreport.repository.UserRepository;
import it.unical.campusreport.service.TicketService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller per la gestione dei ticket di segnalazione.
 */
@RestController
@RequestMapping("/api/tickets")
@Slf4j
public class TicketController {

    private final TicketService ticketService;
    private final UserRepository userRepository;

    public TicketController(TicketService ticketService, UserRepository userRepository) {
        this.ticketService = ticketService;
        this.userRepository = userRepository;
    }

    /**
     * Crea una nuova segnalazione. Accessibile a STUDENTE e DOCENTE.
     *
     * @param request dati del ticket
     * @return ticket creato con status 201
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENTE', 'DOCENTE')")
    public ResponseEntity<TicketResponse> creaTicket(@Valid @RequestBody TicketRequest request) {
        User utente = getCurrentUser();
        log.info("Creazione ticket da parte di {}", utente.getEmail());
        TicketResponse response = ticketService.creaTicket(request, utente);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Restituisce i ticket dell'utente autenticato. Accessibile a STUDENTE e DOCENTE.
     *
     * @return lista ticket dell'utente
     */
    @GetMapping("/miei")
    @PreAuthorize("hasAnyRole('STUDENTE', 'DOCENTE')")
    public ResponseEntity<List<TicketResponse>> getMieiTicket() {
        User utente = getCurrentUser();
        log.debug("Recupero ticket di {}", utente.getEmail());
        return ResponseEntity.ok(ticketService.getTicketsByUser(utente));
    }

    /**
     * Restituisce un ticket per ID con verifica dei permessi in base al ruolo.
     * Accessibile a STUDENTE, DOCENTE, TECNICO e ADMIN.
     *
     * @param id identificativo del ticket
     * @return il ticket richiesto
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENTE', 'DOCENTE', 'TECNICO', 'ADMIN')")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable UUID id) {
        User utente = getCurrentUser();
        log.debug("Recupero ticket {} da parte di {}", id, utente.getEmail());
        return ResponseEntity.ok(ticketService.getTicketById(id, utente));
    }

    /**
     * Restituisce i ticket assegnati al tecnico autenticato. Accessibile a TECNICO.
     *
     * @return lista ticket assegnati
     */
    @GetMapping("/assegnati")
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<List<TicketResponse>> getTicketsAssegnati() {
        User tecnico = getCurrentUser();
        log.debug("Recupero ticket assegnati a {}", tecnico.getEmail());
        return ResponseEntity.ok(ticketService.getTicketsAssegnati(tecnico));
    }

    /**
     * Aggiorna lo stato di un ticket. Accessibile a TECNICO.
     *
     * @param id      identificativo del ticket
     * @param request nuovo stato e nota opzionale
     * @return ticket aggiornato
     */
    @PatchMapping("/{id}/stato")
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<TicketResponse> aggiornaStato(@PathVariable UUID id,
                                                        @Valid @RequestBody AggiornamentoStatoRequest request) {
        User tecnico = getCurrentUser();
        log.info("Tecnico {} aggiorna stato ticket {}", tecnico.getEmail(), id);
        return ResponseEntity.ok(ticketService.aggiornaStato(id, request, tecnico));
    }

    /**
     * Rifiuta un ticket con motivazione obbligatoria. Accessibile a TECNICO.
     *
     * @param id      identificativo del ticket
     * @param request motivazione del rifiuto
     * @return ticket aggiornato con stato RIFIUTATA
     */
    @PostMapping("/{id}/rifiuta")
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<TicketResponse> rifiutaTicket(@PathVariable UUID id,
                                                        @Valid @RequestBody RifiutoRequest request) {
        User tecnico = getCurrentUser();
        log.info("Tecnico {} rifiuta ticket {}", tecnico.getEmail(), id);
        return ResponseEntity.ok(ticketService.rifiutaTicket(id, request, tecnico));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato: " + auth.getName()));
    }
}

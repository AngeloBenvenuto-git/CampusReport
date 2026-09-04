package it.unical.campusreport.controller;

import it.unical.campusreport.dto.*;
import it.unical.campusreport.entity.User;
import it.unical.campusreport.exception.UserNotFoundException;
import it.unical.campusreport.repository.UserRepository;
import it.unical.campusreport.service.AdminConfigService;
import it.unical.campusreport.service.AdminStatisticheService;
import it.unical.campusreport.service.AdminTecnicoService;
import it.unical.campusreport.service.AdminTicketService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller per il pannello di amministrazione: gestione ticket, statistiche,
 * tecnici, assegnazione manuale, esportazione CSV e configurazione dei pesi
 * dell'algoritmo di assegnazione. Tutti gli endpoint sono riservati al ruolo ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {

    private final AdminTicketService adminTicketService;
    private final AdminStatisticheService adminStatisticheService;
    private final AdminTecnicoService adminTecnicoService;
    private final AdminConfigService adminConfigService;
    private final UserRepository userRepository;

    public AdminController(AdminTicketService adminTicketService,
                           AdminStatisticheService adminStatisticheService,
                           AdminTecnicoService adminTecnicoService,
                           AdminConfigService adminConfigService,
                           UserRepository userRepository) {
        this.adminTicketService = adminTicketService;
        this.adminStatisticheService = adminStatisticheService;
        this.adminTecnicoService = adminTecnicoService;
        this.adminConfigService = adminConfigService;
        this.userRepository = userRepository;
    }

    // ─── Ticket ─────────────────────────────────────────────────────────────────

    /**
     * Restituisce tutti i ticket del sistema con filtri opzionali e paginazione.
     *
     * @param filter filtri (stato, categoria, zonaId, tecnicoId, priorita, ricerca) e paginazione
     * @return pagina di ticket filtrati
     */
    @GetMapping("/tickets")
    public ResponseEntity<List<TicketResponse>> getAllTickets(@ModelAttribute AdminTicketFilter filter) {
        log.debug("Admin: ricerca ticket con filtri {}", filter);
        return ResponseEntity.ok(adminTicketService.getAllTickets(filter));
    }

    /**
     * Assegna manualmente un ticket a un tecnico specifico.
     *
     * @param id      identificativo del ticket
     * @param request identificativo del tecnico da assegnare
     * @return ticket aggiornato
     */
    @PatchMapping("/tickets/{id}/assegna")
    public ResponseEntity<TicketResponse> assegnaManualmente(@PathVariable UUID id,
                                                             @Valid @RequestBody AssegnazioneManualRequest request) {
        User admin = getCurrentUser();
        log.info("Admin {} assegna manualmente il ticket {}", admin.getEmail(), id);
        return ResponseEntity.ok(adminTicketService.assegnaManualmente(id, request, admin));
    }

    /**
     * Esporta tutti i ticket del sistema in formato CSV.
     *
     * @return file CSV allegato
     */
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv() {
        log.info("Admin: esportazione CSV ticket");
        byte[] csv = adminTicketService.exportCsv();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tickets.csv\"")
                .body(csv);
    }

    // ─── Statistiche ────────────────────────────────────────────────────────────

    /**
     * Restituisce le statistiche globali del sistema per la dashboard admin.
     *
     * @return statistiche aggregate
     */
    @GetMapping("/statistiche")
    public ResponseEntity<AdminStatisticheResponse> getStatistiche() {
        log.debug("Admin: recupero statistiche globali");
        return ResponseEntity.ok(adminStatisticheService.getStatistiche());
    }

    // ─── Tecnici ────────────────────────────────────────────────────────────────

    /**
     * Restituisce l'elenco di tutti i tecnici con il carico attuale.
     *
     * @return lista tecnici
     */
    @GetMapping("/tecnici")
    public ResponseEntity<List<TecnicoAdminResponse>> getAllTecnici() {
        log.debug("Admin: recupero elenco tecnici");
        return ResponseEntity.ok(adminTecnicoService.getAllTecnici());
    }

    /**
     * Crea un nuovo account tecnico e invia l'email di invito all'attivazione.
     *
     * @param request dati del nuovo tecnico
     * @return tecnico creato con status 201
     */
    @PostMapping("/tecnici")
    public ResponseEntity<TecnicoAdminResponse> creaTecnico(@Valid @RequestBody CreaTecnicoRequest request) {
        log.info("Admin: creazione tecnico {}", request.getEmail());
        TecnicoAdminResponse response = adminTecnicoService.creaTecnico(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Modifica specializzazioni, zona e carico massimo di un tecnico esistente.
     *
     * @param id      identificativo del tecnico
     * @param request nuovi dati
     * @return tecnico aggiornato
     */
    @PutMapping("/tecnici/{id}")
    public ResponseEntity<TecnicoAdminResponse> modificaTecnico(@PathVariable UUID id,
                                                                @Valid @RequestBody ModificaTecnicoRequest request) {
        log.info("Admin: modifica tecnico {}", id);
        return ResponseEntity.ok(adminTecnicoService.modificaTecnico(id, request));
    }

    /**
     * Attiva o disattiva un account tecnico.
     *
     * @param id      identificativo del tecnico
     * @param request nuovo stato di attivazione
     * @return tecnico aggiornato
     */
    @PatchMapping("/tecnici/{id}/stato")
    public ResponseEntity<TecnicoAdminResponse> cambiaStatoTecnico(@PathVariable UUID id,
                                                                   @Valid @RequestBody CambioStatoTecnicoRequest request) {
        log.info("Admin: cambio stato tecnico {} → attivo={}", id, request.isAttivo());
        return ResponseEntity.ok(adminTecnicoService.cambiaStato(id, request));
    }

    // ─── Configurazione algoritmo di assegnazione ──────────────────────────────

    /**
     * Restituisce i pesi correnti dell'algoritmo di assegnazione.
     *
     * @return pesi alpha e beta attuali
     */
    @GetMapping("/config/pesi")
    public ResponseEntity<ConfigPesiResponse> getPesi() {
        log.debug("Admin: recupero pesi assegnazione");
        return ResponseEntity.ok(adminConfigService.getPesi());
    }

    /**
     * Aggiorna a runtime i pesi alpha e beta dell'algoritmo di assegnazione.
     *
     * @param request nuovi pesi (la somma deve essere 1.0)
     * @return pesi aggiornati
     */
    @PostMapping("/config/pesi")
    public ResponseEntity<ConfigPesiResponse> aggiornaPesi(@Valid @RequestBody ConfigPesiRequest request) {
        log.info("Admin: aggiornamento pesi assegnazione, alpha={}, beta={}", request.getAlpha(), request.getBeta());
        return ResponseEntity.ok(adminConfigService.aggiornaPesi(request));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato: " + auth.getName()));
    }
}

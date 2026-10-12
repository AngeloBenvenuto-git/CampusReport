package it.unical.campusreport.controller;

import it.unical.campusreport.dto.ChatResponse;
import it.unical.campusreport.dto.MessaggioRequest;
import it.unical.campusreport.dto.MessaggioResponse;
import it.unical.campusreport.entity.User;
import it.unical.campusreport.exception.UserNotFoundException;
import it.unical.campusreport.repository.UserRepository;
import it.unical.campusreport.service.MessaggioService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller per la chat tra segnalante e tecnico assegnato a un ticket.
 */
@RestController
@RequestMapping("/api/tickets/{id}/chat")
@Slf4j
public class MessaggioController {

    private final MessaggioService messaggioService;
    private final UserRepository userRepository;

    public MessaggioController(MessaggioService messaggioService, UserRepository userRepository) {
        this.messaggioService = messaggioService;
        this.userRepository = userRepository;
    }

    /**
     * Restituisce la chat del ticket e marca come letti i messaggi ricevuti.
     * Accessibile a STUDENTE, DOCENTE e TECNICO.
     *
     * @param id identificativo del ticket
     * @return la conversazione del ticket
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENTE', 'DOCENTE', 'TECNICO')")
    public ResponseEntity<ChatResponse> getChat(@PathVariable UUID id) {
        User utente = getCurrentUser();
        log.debug("Recupero chat ticket {} da parte di {}", id, utente.getEmail());
        return ResponseEntity.ok(messaggioService.getChat(id, utente));
    }

    /**
     * Invia un messaggio nella chat del ticket. Accessibile a STUDENTE, DOCENTE e TECNICO.
     *
     * @param id      identificativo del ticket
     * @param request testo del messaggio
     * @return il messaggio creato con status 201
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENTE', 'DOCENTE', 'TECNICO')")
    public ResponseEntity<MessaggioResponse> inviaMessaggio(@PathVariable UUID id,
                                                            @Valid @RequestBody MessaggioRequest request) {
        User utente = getCurrentUser();
        log.info("Utente {} invia messaggio sul ticket {}", utente.getEmail(), id);
        return ResponseEntity.status(HttpStatus.CREATED).body(messaggioService.inviaMessaggio(id, request, utente));
    }

    /**
     * Restituisce il numero di messaggi non letti nella chat del ticket, senza
     * marcarli come letti. Accessibile a STUDENTE, DOCENTE e TECNICO.
     *
     * @param id identificativo del ticket
     * @return oggetto JSON con il campo {@code nonLetti}
     */
    @GetMapping("/non-letti")
    @PreAuthorize("hasAnyRole('STUDENTE', 'DOCENTE', 'TECNICO')")
    public ResponseEntity<Map<String, Long>> getNonLetti(@PathVariable UUID id) {
        User utente = getCurrentUser();
        return ResponseEntity.ok(Map.of("nonLetti", messaggioService.contaNonLetti(id, utente)));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato: " + auth.getName()));
    }
}

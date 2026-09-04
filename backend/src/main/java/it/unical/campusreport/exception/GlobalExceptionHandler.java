package it.unical.campusreport.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestore globale delle eccezioni. Intercetta le eccezioni custom del progetto
 * e restituisce risposte JSON strutturate con error, message e timestamp.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Gestisce il tentativo di registrazione con un'email già esistente.
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Email già esistente", ex.getMessage());
    }

    /**
     * Gestisce l'uso di un dominio email non consentito.
     */
    @ExceptionHandler(InvalidDomainException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidDomain(InvalidDomainException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Dominio email non valido", ex.getMessage());
    }

    /**
     * Gestisce la ricerca di un utente inesistente.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Utente non trovato", ex.getMessage());
    }

    /**
     * Gestisce il tentativo di accesso con un account non attivo.
     */
    @ExceptionHandler(UserNotActiveException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotActive(UserNotActiveException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Account non attivo", ex.getMessage());
    }

    /**
     * Gestisce credenziali di login errate.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Credenziali non valide", ex.getMessage());
    }

    /**
     * Gestisce la ricerca di un ticket inesistente.
     */
    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTicketNotFound(TicketNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Ticket non trovato", ex.getMessage());
    }

    /**
     * Gestisce il tentativo di accesso non autorizzato a un ticket altrui.
     */
    @ExceptionHandler(UnauthorizedTicketAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedTicketAccess(UnauthorizedTicketAccessException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Accesso non autorizzato al ticket", ex.getMessage());
    }

    /**
     * Gestisce una transizione di stato non consentita dal workflow.
     */
    @ExceptionHandler(InvalidStatoTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidStatoTransition(InvalidStatoTransitionException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Transizione di stato non valida", ex.getMessage());
    }

    /**
     * Gestisce la ricerca di una zona inesistente.
     */
    @ExceptionHandler(ZonaNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleZonaNotFound(ZonaNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Zona non trovata", ex.getMessage());
    }

    /**
     * Gestisce la ricerca di un tecnico inesistente.
     */
    @ExceptionHandler(TecnicoNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTecnicoNotFound(TecnicoNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Tecnico non trovato", ex.getMessage());
    }

    /**
     * Gestisce il tentativo di assegnare un ticket a un tecnico non attivo.
     */
    @ExceptionHandler(TecnicoNonAttivoException.class)
    public ResponseEntity<Map<String, Object>> handleTecnicoNonAttivo(TecnicoNonAttivoException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Tecnico non attivo", ex.getMessage());
    }

    /**
     * Gestisce pesi alpha/beta non validi per l'algoritmo di assegnazione.
     */
    @ExceptionHandler(ConfigPesiInvalidiException.class)
    public ResponseEntity<Map<String, Object>> handleConfigPesiInvalidi(ConfigPesiInvalidiException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Pesi non validi", ex.getMessage());
    }

    /**
     * Gestisce conflitti di aggiornamento concorrente (optimistic locking).
     * Restituisce 409 Conflict per indicare che il ticket è stato modificato da un altro processo.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.warn("Conflitto di aggiornamento concorrente: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "Conflitto di aggiornamento",
                "Il ticket è stato modificato da un altro processo. Ricaricare e riprovare.");
    }

    /**
     * Gestisce gli errori di validazione dei DTO (campi @NotBlank, @Email, ecc.).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Errore di validazione", errors);
    }

    /**
     * Fallback per eccezioni non gestite esplicitamente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Errore non gestito: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno",
                "Si è verificato un errore imprevisto");
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status,
                                                                    String error,
                                                                    String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}

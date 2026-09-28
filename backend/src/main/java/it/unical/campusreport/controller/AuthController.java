package it.unical.campusreport.controller;

import it.unical.campusreport.dto.AttivaAccountRequest;
import it.unical.campusreport.dto.AuthResponse;
import it.unical.campusreport.dto.LoginRequest;
import it.unical.campusreport.dto.RegisterRequest;
import it.unical.campusreport.dto.VerificaTokenResponse;
import it.unical.campusreport.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller per le operazioni di autenticazione: registrazione, login e attivazione account tecnico.
 * Gli endpoint sono pubblici (non richiedono JWT).
 */
@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registra un nuovo utente (STUDENTE o DOCENTE) nel sistema.
     *
     * @param request dati di registrazione validati
     * @return 201 Created con token JWT e dati utente
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Richiesta registrazione per email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Autentica un utente esistente con email e password.
     *
     * @param request credenziali di login validate
     * @return 200 OK con token JWT e dati utente
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Richiesta login per email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Attiva l'account di un tecnico impostando la password tramite il token di invito.
     *
     * @param request token, password e conferma password validati
     * @return 200 OK con token JWT e dati utente (il tecnico risulta subito autenticato)
     */
    @PostMapping("/attiva")
    public ResponseEntity<AuthResponse> attivaAccount(@Valid @RequestBody AttivaAccountRequest request) {
        return ResponseEntity.ok(authService.attivaAccount(request));
    }

    /**
     * Verifica se un token di attivazione è valido prima che il tecnico compili il form.
     *
     * @param token il token ricevuto nel link di invito
     * @return 200 OK con esito della verifica e, se valido, nome, cognome ed email del tecnico
     */
    @GetMapping("/verifica-token")
    public ResponseEntity<VerificaTokenResponse> verificaToken(@RequestParam String token) {
        return ResponseEntity.ok(authService.verificaToken(token));
    }
}

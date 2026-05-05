package it.unical.campusreport.controller;

import it.unical.campusreport.dto.AuthResponse;
import it.unical.campusreport.dto.LoginRequest;
import it.unical.campusreport.dto.RegisterRequest;
import it.unical.campusreport.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller per le operazioni di autenticazione: registrazione e login.
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
}

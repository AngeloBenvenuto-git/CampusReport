package it.unical.campusreport.service;

import it.unical.campusreport.dto.AuthResponse;
import it.unical.campusreport.dto.LoginRequest;
import it.unical.campusreport.dto.RegisterRequest;

/**
 * Interfaccia del servizio di autenticazione.
 */
public interface AuthService {

    /**
     * Registra un nuovo utente nel sistema. Determina il ruolo in base al dominio email
     * (in base al profilo attivo) e restituisce un token JWT.
     *
     * @param request dati di registrazione (nome, cognome, email, password)
     * @return risposta con token JWT e dati dell'utente
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Autentica un utente esistente verificando email e password.
     * Restituisce un token JWT se le credenziali sono valide.
     *
     * @param request credenziali di login (email, password)
     * @return risposta con token JWT e dati dell'utente
     */
    AuthResponse login(LoginRequest request);
}

package it.unical.campusreport.service;

import it.unical.campusreport.dto.AttivaAccountRequest;
import it.unical.campusreport.dto.AuthResponse;
import it.unical.campusreport.dto.LoginRequest;
import it.unical.campusreport.dto.RegisterRequest;
import it.unical.campusreport.dto.VerificaTokenResponse;

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

    /**
     * Attiva l'account di un tecnico tramite il token di invito: imposta la password,
     * marca l'account come attivo e il token come usato. Restituisce un token JWT
     * così che il tecnico risulti subito autenticato.
     *
     * @param request token di attivazione, password e conferma password
     * @return risposta con token JWT e dati dell'utente attivato
     */
    AuthResponse attivaAccount(AttivaAccountRequest request);

    /**
     * Verifica se un token di attivazione è utilizzabile (esistente, non usato, non scaduto).
     *
     * @param token il token da verificare
     * @return esito della verifica con i dati dell'utente se valido, altrimenti il motivo
     */
    VerificaTokenResponse verificaToken(String token);
}

package it.unical.campusreport.exception;

/**
 * Eccezione lanciata quando un utente tenta di accedere con un account disabilitato.
 * Corrisponde a HTTP 403 Forbidden.
 */
public class UserNotActiveException extends RuntimeException {

    public UserNotActiveException(String message) {
        super(message);
    }
}

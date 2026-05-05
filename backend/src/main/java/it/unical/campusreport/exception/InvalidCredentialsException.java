package it.unical.campusreport.exception;

/**
 * Eccezione lanciata quando le credenziali di login non sono corrette.
 * Corrisponde a HTTP 401 Unauthorized.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}

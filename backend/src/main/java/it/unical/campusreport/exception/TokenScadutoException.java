package it.unical.campusreport.exception;

/**
 * Lanciata quando il token di attivazione è scaduto.
 */
public class TokenScadutoException extends RuntimeException {
    public TokenScadutoException(String message) {
        super(message);
    }
}

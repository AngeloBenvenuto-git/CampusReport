package it.unical.campusreport.exception;

/**
 * Lanciata quando il token di attivazione non esiste o è già stato utilizzato.
 */
public class TokenNonValidoException extends RuntimeException {
    public TokenNonValidoException(String message) {
        super(message);
    }
}

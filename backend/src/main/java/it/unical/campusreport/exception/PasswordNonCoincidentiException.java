package it.unical.campusreport.exception;

/**
 * Lanciata quando la password e la sua conferma non coincidono.
 */
public class PasswordNonCoincidentiException extends RuntimeException {
    public PasswordNonCoincidentiException(String message) {
        super(message);
    }
}

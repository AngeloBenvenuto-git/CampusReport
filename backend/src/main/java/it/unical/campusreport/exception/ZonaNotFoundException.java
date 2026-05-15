package it.unical.campusreport.exception;

/**
 * Lanciata quando una zona richiesta non esiste nel sistema.
 */
public class ZonaNotFoundException extends RuntimeException {
    public ZonaNotFoundException(String message) {
        super(message);
    }
}

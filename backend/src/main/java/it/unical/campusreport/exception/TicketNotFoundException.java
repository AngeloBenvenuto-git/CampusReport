package it.unical.campusreport.exception;

/**
 * Lanciata quando un ticket richiesto non esiste nel sistema.
 */
public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(String message) {
        super(message);
    }
}

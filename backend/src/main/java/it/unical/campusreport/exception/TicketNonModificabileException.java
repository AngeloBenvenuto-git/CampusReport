package it.unical.campusreport.exception;

/**
 * Lanciata quando il segnalante tenta di modificare un ticket il cui stato
 * non consente più la modifica (stati modificabili: APERTA e ASSEGNATA).
 */
public class TicketNonModificabileException extends RuntimeException {
    public TicketNonModificabileException(String message) {
        super(message);
    }
}

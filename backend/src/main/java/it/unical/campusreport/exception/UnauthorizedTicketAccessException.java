package it.unical.campusreport.exception;

/**
 * Lanciata quando un utente tenta di accedere a un ticket che non gli appartiene.
 */
public class UnauthorizedTicketAccessException extends RuntimeException {
    public UnauthorizedTicketAccessException(String message) {
        super(message);
    }
}

package it.unical.campusreport.exception;

/**
 * Lanciata quando si tenta una transizione di stato non consentita dal workflow.
 */
public class InvalidStatoTransitionException extends RuntimeException {
    public InvalidStatoTransitionException(String message) {
        super(message);
    }
}

package it.unical.campusreport.exception;

/**
 * Lanciata quando si tenta di accedere alla chat di un ticket che non ha
 * ancora un tecnico assegnato.
 */
public class ChatNonDisponibileException extends RuntimeException {
    public ChatNonDisponibileException(String message) {
        super(message);
    }
}

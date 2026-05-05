package it.unical.campusreport.exception;

/**
 * Eccezione lanciata quando si tenta di registrare un'email già presente nel sistema.
 * Corrisponde a HTTP 409 Conflict.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}

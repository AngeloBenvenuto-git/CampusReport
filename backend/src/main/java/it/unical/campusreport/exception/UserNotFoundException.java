package it.unical.campusreport.exception;

/**
 * Eccezione lanciata quando un utente non viene trovato nel database.
 * Corrisponde a HTTP 404 Not Found.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}

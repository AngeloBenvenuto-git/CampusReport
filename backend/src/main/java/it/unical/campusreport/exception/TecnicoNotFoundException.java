package it.unical.campusreport.exception;

/**
 * Lanciata quando un tecnico richiesto non esiste nel sistema.
 * Corrisponde a HTTP 404 Not Found.
 */
public class TecnicoNotFoundException extends RuntimeException {

    public TecnicoNotFoundException(String message) {
        super(message);
    }
}

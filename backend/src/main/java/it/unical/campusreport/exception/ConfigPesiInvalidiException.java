package it.unical.campusreport.exception;

/**
 * Lanciata quando la somma dei pesi alpha e beta dell'algoritmo di assegnazione
 * non è uguale a 1.0. Corrisponde a HTTP 400 Bad Request.
 */
public class ConfigPesiInvalidiException extends RuntimeException {

    public ConfigPesiInvalidiException(String message) {
        super(message);
    }
}

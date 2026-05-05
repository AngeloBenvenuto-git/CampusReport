package it.unical.campusreport.exception;

/**
 * Eccezione lanciata quando il dominio email non è tra quelli consentiti.
 * Corrisponde a HTTP 400 Bad Request.
 */
public class InvalidDomainException extends RuntimeException {

    public InvalidDomainException(String message) {
        super(message);
    }
}

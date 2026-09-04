package it.unical.campusreport.exception;

/**
 * Lanciata quando si tenta di assegnare un ticket a un tecnico non attivo.
 * Corrisponde a HTTP 409 Conflict.
 */
public class TecnicoNonAttivoException extends RuntimeException {

    public TecnicoNonAttivoException(String message) {
        super(message);
    }
}

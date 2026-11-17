package it.unical.campusreport.exception;

/**
 * Lanciata quando l'amministratore tenta di eliminare un tecnico ancora attivo
 * o con segnalazioni in corso (ASSEGNATA o IN_LAVORAZIONE).
 */
public class TecnicoNonEliminabileException extends RuntimeException {
    public TecnicoNonEliminabileException(String message) {
        super(message);
    }
}

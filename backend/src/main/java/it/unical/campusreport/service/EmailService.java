package it.unical.campusreport.service;

import it.unical.campusreport.entity.Ticket;
import it.unical.campusreport.entity.User;
import it.unical.campusreport.entity.enums.Stato;

/**
 * Servizio per l'invio delle notifiche email agli attori del sistema
 * (tecnici, segnalanti, amministratore) sui principali eventi del
 * ciclo di vita di una segnalazione.
 *
 * <p>Le implementazioni non devono propagare eccezioni: un errore di
 * invio email non deve mai bloccare il flusso applicativo principale.
 */
public interface EmailService {

    /**
     * Notifica un tecnico che gli è stata assegnata una nuova segnalazione.
     *
     * @param ticket  il ticket appena assegnato
     * @param tecnico il tecnico destinatario della notifica
     */
    void notificaTecnicoNuovaAssegnazione(Ticket ticket, User tecnico);

    /**
     * Notifica il segnalante che la sua segnalazione ha cambiato stato.
     *
     * @param ticket     il ticket il cui stato è cambiato
     * @param statoNuovo il nuovo stato del ticket
     */
    void notificaUtenteStatoCambiato(Ticket ticket, Stato statoNuovo);

    /**
     * Notifica l'amministratore che un ticket non è stato assegnato
     * automaticamente a nessun tecnico ed è passato in stato IN_ATTESA.
     *
     * @param ticket il ticket rimasto senza tecnico disponibile
     */
    void notificaAdminTicketInAttesa(Ticket ticket);

    /**
     * Invia a un tecnico appena creato dall'admin il link di attivazione
     * dell'account (valido 48 ore) per impostare la propria password.
     *
     * @param tecnico l'account tecnico appena creato, non ancora attivo
     * @param token   il token di attivazione da includere nel link
     */
    void inviaInvitoTecnico(User tecnico, String token);
}

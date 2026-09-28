package it.unical.campusreport.service;

import it.unical.campusreport.entity.Ticket;
import it.unical.campusreport.entity.User;
import it.unical.campusreport.entity.enums.Categoria;
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

    /**
     * Notifica il segnalante che la sua segnalazione è stata rifiutata
     * definitivamente dal tecnico ed eliminata dal sistema.
     *
     * @param ticket      il ticket rifiutato (non ancora eliminato al momento della chiamata)
     * @param motivazione il motivo del rifiuto fornito dal tecnico
     */
    void notificaRifiutoDefinitivo(Ticket ticket, String motivazione);

    /**
     * Notifica il tecnico assegnato che il segnalante ha modificato i dettagli
     * della segnalazione a lui assegnata, riportando i valori precedenti e
     * quelli aggiornati.
     *
     * @param ticket             il ticket modificato, già aggiornato con i nuovi valori
     * @param vecchioTitolo      il titolo prima della modifica
     * @param vecchiaDescrizione la descrizione prima della modifica
     * @param vecchiaCategoria   la categoria prima della modifica
     * @param vecchioCubo        il cubo prima della modifica
     * @param vecchioPiano       il piano prima della modifica
     */
    void notificaTecnicoModificaSegnalazione(Ticket ticket,
                                              String vecchioTitolo,
                                              String vecchiaDescrizione,
                                              Categoria vecchiaCategoria,
                                              String vecchioCubo,
                                              String vecchioPiano);
}

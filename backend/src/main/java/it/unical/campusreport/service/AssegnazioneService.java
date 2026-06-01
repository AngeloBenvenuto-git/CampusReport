package it.unical.campusreport.service;

import it.unical.campusreport.entity.Ticket;

import java.util.UUID;

/**
 * Servizio responsabile dell'assegnazione automatica dei ticket ai tecnici.
 * Applica la formula: score = α·match_specializzazione + β·(1 − carico_attuale).
 */
public interface AssegnazioneService {

    /**
     * Tenta di assegnare il tecnico ottimale al ticket.
     * Modifica il ticket in-place (stato, tecnico).
     * Se nessun tecnico è disponibile, imposta lo stato a IN_ATTESA.
     *
     * @param ticket il ticket da assegnare (stato atteso: APERTA)
     */
    void assegna(Ticket ticket);

    /**
     * Riesegue l'assegnazione escludendo un tecnico specifico.
     * Usato dopo il rifiuto di un tecnico.
     * Modifica il ticket in-place (stato, tecnico).
     *
     * @param ticket          il ticket da riassegnare (stato atteso: RIFIUTATA)
     * @param tecnicoEscluso  UUID del tecnico che ha rifiutato, da escludere
     */
    void riassegna(Ticket ticket, UUID tecnicoEscluso);
}

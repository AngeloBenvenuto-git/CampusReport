package it.unical.campusreport.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Matrice di adiacenza (simmetrica) tra le macro-zone del campus, usata come
 * quarto criterio dell'algoritmo di assegnazione.
 *
 * <p>Valori di vicinanza: 1.0 stessa zona, 0.7 zone confinanti, 0.4 zone vicine,
 * 0.1 per ogni altra combinazione.
 */
@Service
@Slf4j
public class MatriceAdiacenzaService {

    static final double VICINANZA_STESSA_ZONA = 1.0;
    static final double VICINANZA_ADIACENTE = 0.7;
    static final double VICINANZA_VICINA = 0.4;
    static final double VICINANZA_LONTANA = 0.1;

    private static final String POLO_UMANISTICO = "Polo Umanistico";
    private static final String POLO_SCIENTIFICO_OVEST = "Polo Scientifico Ovest";
    private static final String POLO_SCIENTIFICO_EST = "Polo Scientifico Est";
    private static final String BIBLIOTECA_RETTORATO = "Biblioteca e Rettorato";
    private static final String POLO_INGEGNERIA = "Polo Ingegneria";
    private static final String POLIFUNZIONALE = "Polifunzionale";
    private static final String TAU_CINEMA = "TAU Cinema Campus";
    private static final String PTU_RESIDENZIALE = "PTU e Centro Residenziale";
    private static final String AULA_CALDORA = "Aula Caldora";
    private static final String CENTRO_SPORTIVO = "Centro Sportivo";
    private static final String POLO_INNOVAZIONE = "Polo di Innovazione";
    private static final String CENTRO_CONGRESSI = "Centro Congressi e Aula Magna";

    private final Map<String, Map<String, Double>> matrice = new HashMap<>();

    public MatriceAdiacenzaService() {
        // Vicinanza 0.7 — zone confinanti
        collega(POLO_UMANISTICO, POLO_SCIENTIFICO_OVEST, VICINANZA_ADIACENTE);
        collega(POLO_SCIENTIFICO_OVEST, CENTRO_CONGRESSI, VICINANZA_ADIACENTE);
        collega(POLO_SCIENTIFICO_OVEST, BIBLIOTECA_RETTORATO, VICINANZA_ADIACENTE);
        collega(BIBLIOTECA_RETTORATO, POLO_SCIENTIFICO_EST, VICINANZA_ADIACENTE);
        collega(POLO_SCIENTIFICO_EST, POLO_INGEGNERIA, VICINANZA_ADIACENTE);
        collega(POLO_INGEGNERIA, TAU_CINEMA, VICINANZA_ADIACENTE);
        collega(AULA_CALDORA, PTU_RESIDENZIALE, VICINANZA_ADIACENTE);

        // Vicinanza 0.4 — zone vicine
        collega(AULA_CALDORA, CENTRO_CONGRESSI, VICINANZA_VICINA);
        collega(CENTRO_SPORTIVO, AULA_CALDORA, VICINANZA_VICINA);
        collega(CENTRO_SPORTIVO, PTU_RESIDENZIALE, VICINANZA_VICINA);
        collega(POLO_UMANISTICO, AULA_CALDORA, VICINANZA_VICINA);
        collega(POLO_UMANISTICO, POLIFUNZIONALE, VICINANZA_VICINA);
        collega(POLO_UMANISTICO, CENTRO_CONGRESSI, VICINANZA_VICINA);
        collega(POLO_UMANISTICO, BIBLIOTECA_RETTORATO, VICINANZA_VICINA);
        collega(POLO_UMANISTICO, PTU_RESIDENZIALE, VICINANZA_VICINA);
        collega(POLO_SCIENTIFICO_OVEST, PTU_RESIDENZIALE, VICINANZA_VICINA);
        collega(POLO_SCIENTIFICO_OVEST, POLO_SCIENTIFICO_EST, VICINANZA_VICINA);
        collega(BIBLIOTECA_RETTORATO, POLO_INGEGNERIA, VICINANZA_VICINA);
        collega(POLO_SCIENTIFICO_EST, TAU_CINEMA, VICINANZA_VICINA);
        collega(POLO_SCIENTIFICO_EST, POLO_INNOVAZIONE, VICINANZA_VICINA);
        collega(POLO_INGEGNERIA, POLO_INNOVAZIONE, VICINANZA_VICINA);

        log.debug("Matrice di adiacenza zone inizializzata con {} zone", matrice.size());
    }

    /**
     * Calcola la vicinanza tra la zona del tecnico e la zona del ticket.
     *
     * <p>La matrice è simmetrica, quindi l'ordine dei parametri non influisce sul risultato.
     *
     * @param zonaTecnico nome della zona di competenza del tecnico
     * @param zonaTicket  nome della zona in cui si trova la segnalazione
     * @return 1.0 se le zone coincidono, 0.7 o 0.4 se sono confinanti/vicine secondo
     *         la matrice, 0.1 in tutti gli altri casi (incluse zone sconosciute o null)
     */
    public double calcolaVicinanza(String zonaTecnico, String zonaTicket) {
        if (zonaTecnico == null || zonaTicket == null) {
            return VICINANZA_LONTANA;
        }
        if (zonaTecnico.equals(zonaTicket)) {
            return VICINANZA_STESSA_ZONA;
        }
        Double valore = matrice.getOrDefault(zonaTecnico, Map.of()).get(zonaTicket);
        if (valore == null) {
            valore = matrice.getOrDefault(zonaTicket, Map.of()).get(zonaTecnico);
        }
        return valore != null ? valore : VICINANZA_LONTANA;
    }

    /** Inserisce il collegamento in entrambe le direzioni (matrice simmetrica). */
    private void collega(String zonaA, String zonaB, double vicinanza) {
        matrice.computeIfAbsent(zonaA, k -> new HashMap<>()).put(zonaB, vicinanza);
        matrice.computeIfAbsent(zonaB, k -> new HashMap<>()).put(zonaA, vicinanza);
    }
}

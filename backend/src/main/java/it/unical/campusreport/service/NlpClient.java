package it.unical.campusreport.service;

import it.unical.campusreport.dto.NlpResponse;
import it.unical.campusreport.dto.NlpUrgenzaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client HTTP verso il microservizio Python NLP (FastAPI su porta 8000).
 * In caso di errore di rete o timeout restituisce un fallback graceful
 * senza bloccare la creazione del ticket.
 */
@Component
@Slf4j
public class NlpClient {

    /** Urgenza media usata quando il microservizio NLP non risponde. */
    public static final int URGENZA_DEFAULT = 3;

    private final RestTemplate restTemplate;
    private final String nlpUrl;


    public NlpClient(@Value("${app.nlp.url}") String nlpUrl) {
        // Timeout di 5 secondi: se NLP non risponde si usa il fallback ALTRO
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(5_000);
        this.restTemplate = new RestTemplate(factory);
        this.nlpUrl = nlpUrl;
    }

    /**
     * Invia il testo al microservizio NLP e restituisce la categoria classificata.
     * Se il servizio non è raggiungibile restituisce categoria ALTRO con confidenza 0.0.
     */
    public NlpResponse classifyText(String testo) {
        try {
            log.debug("Chiamata NLP /classify per testo di {} caratteri", testo.length());
            Map<String, String> body = Map.of("testo", testo);
            NlpResponse response = restTemplate.postForObject(
                    nlpUrl + "/classify", body, NlpResponse.class);
            if (response != null) {
                log.debug("NLP risposta: categoria={}, confidenza={}",
                        response.getCategoria(), response.getConfidenza());
            }
            return response != null ? response : fallback();
        } catch (Exception e) {
            log.warn("Servizio NLP non disponibile, uso fallback ALTRO: {}", e.getMessage());
            return fallback();
        }
    }

    /**
     * Invia il testo al microservizio NLP e restituisce l'urgenza stimata (scala 1-5).
     * Se il servizio non è raggiungibile (o la risposta non è valida) restituisce
     * urgenza media 3 con confidenza 0.0.
     */
    public NlpUrgenzaResponse classifyUrgenza(String testo) {
        try {
            log.debug("Chiamata NLP /classify-urgenza per testo di {} caratteri", testo.length());
            Map<String, String> body = Map.of("testo", testo);
            NlpUrgenzaResponse response = restTemplate.postForObject(
                    nlpUrl + "/classify-urgenza", body, NlpUrgenzaResponse.class);
            if (response == null || response.getUrgenza() < 1 || response.getUrgenza() > 5) {
                log.warn("Risposta NLP urgenza non valida, uso fallback urgenza {}", URGENZA_DEFAULT);
                return fallbackUrgenza();
            }
            log.debug("NLP risposta urgenza: urgenza={}, confidenza={}",
                    response.getUrgenza(), response.getConfidenza());
            return response;
        } catch (Exception e) {
            log.warn("Servizio NLP urgenza non disponibile, uso fallback urgenza {}: {}",
                    URGENZA_DEFAULT, e.getMessage());
            return fallbackUrgenza();
        }
    }

    /**
     * Controlla se il microservizio NLP è operativo.
     */
    public boolean isHealthy() {
        try {
            Map<?, ?> response = restTemplate.getForObject(nlpUrl + "/health", Map.class);
            return response != null && "ok".equals(response.get("status"));
        } catch (Exception e) {
            log.warn("Health check NLP fallito: {}", e.getMessage());
            return false;
        }
    }

    private NlpResponse fallback() {
        NlpResponse fallback = new NlpResponse();
        fallback.setCategoria("ALTRO");
        fallback.setConfidenza(0.0f);
        return fallback;
    }

    private NlpUrgenzaResponse fallbackUrgenza() {
        NlpUrgenzaResponse fallback = new NlpUrgenzaResponse();
        fallback.setUrgenza(URGENZA_DEFAULT);
        fallback.setConfidenza(0.0f);
        fallback.setDescrizione("urgenza media (default)");
        return fallback;
    }
}

package it.unical.campusreport.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configurazione dell'algoritmo di assegnazione automatica dei ticket.
 * Legge i pesi alpha e beta da {@code app.assignment.*} in application.yml.
 * Verifica all'avvio che alpha + beta == 1.0.
 */
@Configuration
@Validated
@ConfigurationProperties(prefix = "app.assignment")
@Getter
@Setter
public class AssegnazioneConfig {

    /** Peso per la corrispondenza di specializzazione (default 0.6). */
    private double alpha = 0.6;

    /** Peso per il carico di lavoro (default 0.4). */
    private double beta = 0.4;

    @PostConstruct
    public void validate() {
        double somma = alpha + beta;
        if (Math.abs(somma - 1.0) > 1e-9) {
            throw new IllegalStateException(
                    String.format(
                            "app.assignment.alpha + app.assignment.beta deve essere 1.0, trovato: %.6f",
                            somma));
        }
    }
}

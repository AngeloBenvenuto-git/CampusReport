package it.unical.campusreport.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configurazione dell'algoritmo di assegnazione automatica dei ticket.
 * Legge i pesi alpha, beta, gamma e delta da {@code app.assignment.*} in application.yml.
 * Verifica all'avvio che alpha + beta + gamma + delta == 1.0.
 */
@Configuration
@Validated
@ConfigurationProperties(prefix = "app.assignment")
@Getter
@Setter
public class AssegnazioneConfig {

    /** Peso per la corrispondenza di specializzazione (default 0.5). */
    private double alpha = 0.5;

    /** Peso per il carico di lavoro (default 0.3). */
    private double beta = 0.3;

    /** Peso per l'urgenza della segnalazione (default 0.1). */
    private double gamma = 0.1;

    /** Peso per la vicinanza tra zona del tecnico e zona del ticket (default 0.1). */
    private double delta = 0.1;

    @PostConstruct
    public void validate() {
        double somma = alpha + beta + gamma + delta;
        if (Math.abs(somma - 1.0) > 1e-9) {
            throw new IllegalStateException(
                    String.format(
                            "app.assignment.alpha + beta + gamma + delta deve essere 1.0, trovato: %.6f",
                            somma));
        }
    }
}

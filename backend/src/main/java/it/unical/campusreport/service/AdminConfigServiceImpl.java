package it.unical.campusreport.service;

import it.unical.campusreport.config.AssegnazioneConfig;
import it.unical.campusreport.dto.ConfigPesiRequest;
import it.unical.campusreport.dto.ConfigPesiResponse;
import it.unical.campusreport.exception.ConfigPesiInvalidiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementazione di {@link AdminConfigService}. Modifica direttamente il bean
 * {@link AssegnazioneConfig}, condiviso a runtime con {@link AssegnazioneServiceImpl}.
 */
@Service
@Slf4j
public class AdminConfigServiceImpl implements AdminConfigService {

    private static final double TOLLERANZA_SOMMA = 1e-9;

    private final AssegnazioneConfig assegnazioneConfig;

    public AdminConfigServiceImpl(AssegnazioneConfig assegnazioneConfig) {
        this.assegnazioneConfig = assegnazioneConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigPesiResponse getPesi() {
        return toResponse();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigPesiResponse aggiornaPesi(ConfigPesiRequest request) {
        double somma = request.getAlpha() + request.getBeta() + request.getGamma() + request.getDelta();
        if (Math.abs(somma - 1.0) > TOLLERANZA_SOMMA) {
            throw new ConfigPesiInvalidiException(
                    String.format("alpha + beta + gamma + delta deve essere uguale a 1.0, trovato: %.6f", somma));
        }

        assegnazioneConfig.setAlpha(request.getAlpha());
        assegnazioneConfig.setBeta(request.getBeta());
        assegnazioneConfig.setGamma(request.getGamma());
        assegnazioneConfig.setDelta(request.getDelta());

        log.info("Pesi assegnazione aggiornati: alpha={}, beta={}, gamma={}, delta={}",
                request.getAlpha(), request.getBeta(), request.getGamma(), request.getDelta());
        return toResponse();
    }

    private ConfigPesiResponse toResponse() {
        return new ConfigPesiResponse(
                assegnazioneConfig.getAlpha(), assegnazioneConfig.getBeta(),
                assegnazioneConfig.getGamma(), assegnazioneConfig.getDelta());
    }
}

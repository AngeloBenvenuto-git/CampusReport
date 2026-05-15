package it.unical.campusreport.service;

import it.unical.campusreport.dto.ZonaResponse;
import it.unical.campusreport.entity.Zona;
import it.unical.campusreport.exception.ZonaNotFoundException;
import it.unical.campusreport.repository.ZonaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementazione di {@link ZonaService}.
 */
@Service
@Slf4j
public class ZonaServiceImpl implements ZonaService {

    private final ZonaRepository zonaRepository;

    public ZonaServiceImpl(ZonaRepository zonaRepository) {
        this.zonaRepository = zonaRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<ZonaResponse> getAllZone() {
        log.debug("Recupero tutte le zone");
        return zonaRepository.findAll().stream()
                .map(this::toZonaResponse)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public ZonaResponse getZonaById(UUID id) {
        log.debug("Recupero zona con id: {}", id);
        Zona zona = zonaRepository.findById(id)
                .orElseThrow(() -> new ZonaNotFoundException("Zona non trovata con id: " + id));
        return toZonaResponse(zona);
    }

    /**
     * Converte un'entità Zona in ZonaResponse.
     *
     * @param zona entità da convertire
     * @return DTO di risposta
     */
    private ZonaResponse toZonaResponse(Zona zona) {
        return ZonaResponse.builder()
                .id(zona.getId())
                .nome(zona.getNome())
                .descrizione(zona.getDescrizione())
                .geojson(zona.getGeojson())
                .colore(zona.getColore())
                .build();
    }
}

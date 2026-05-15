package it.unical.campusreport.controller;

import it.unical.campusreport.dto.ZonaResponse;
import it.unical.campusreport.service.ZonaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller per le zone del campus. Espone la mappa delle zone planimetriche.
 */
@RestController
@RequestMapping("/api/map")
@Slf4j
public class ZonaController {

    private final ZonaService zonaService;

    public ZonaController(ZonaService zonaService) {
        this.zonaService = zonaService;
    }

    /**
     * Restituisce tutte le zone del campus. Accessibile a qualsiasi utente autenticato.
     *
     * @return lista di tutte le zone
     */
    @GetMapping("/zone")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ZonaResponse>> getAllZone() {
        log.debug("Richiesta lista zone");
        return ResponseEntity.ok(zonaService.getAllZone());
    }
}

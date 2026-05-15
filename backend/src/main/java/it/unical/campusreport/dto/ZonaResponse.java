package it.unical.campusreport.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * DTO di risposta per una zona del campus.
 */
@Data
@Builder
public class ZonaResponse {
    private UUID id;
    private String nome;
    private String descrizione;
    private String geojson;
    private String colore;
}

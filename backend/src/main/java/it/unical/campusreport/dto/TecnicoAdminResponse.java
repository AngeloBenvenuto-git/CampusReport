package it.unical.campusreport.dto;

import it.unical.campusreport.entity.enums.Categoria;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * DTO di risposta con i dati di un tecnico e il suo carico attuale, per il pannello admin.
 */
@Data
@Builder
public class TecnicoAdminResponse {
    private UUID id;
    private String nome;
    private String cognome;
    private String email;
    private List<Categoria> specializzazioni;
    private String zona;
    private int caricoMassimo;
    private int caricoAttuale;
    private boolean attivo;
}

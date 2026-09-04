package it.unical.campusreport.dto;

import it.unical.campusreport.entity.enums.Categoria;
import it.unical.campusreport.entity.enums.Priorita;
import it.unical.campusreport.entity.enums.Stato;
import lombok.Data;

import java.util.UUID;

/**
 * Filtri opzionali per la ricerca paginata dei ticket nel pannello admin.
 * Popolato dai query params della richiesta GET /api/admin/tickets.
 */
@Data
public class AdminTicketFilter {

    private Stato stato;
    private Categoria categoria;
    private UUID zonaId;
    private UUID tecnicoId;
    private Priorita priorita;
    private String ricerca;

    private int page = 0;
    private int size = 20;
}

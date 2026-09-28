package it.unical.campusreport.service;

import it.unical.campusreport.dto.*;
import it.unical.campusreport.entity.CambioStato;
import it.unical.campusreport.entity.Tecnico;
import it.unical.campusreport.entity.Ticket;
import it.unical.campusreport.entity.User;
import it.unical.campusreport.entity.Zona;
import it.unical.campusreport.entity.enums.Priorita;
import it.unical.campusreport.entity.enums.Stato;
import it.unical.campusreport.exception.TecnicoNonAttivoException;
import it.unical.campusreport.exception.TecnicoNotFoundException;
import it.unical.campusreport.exception.TicketNotFoundException;
import it.unical.campusreport.repository.CambioStatoRepository;
import it.unical.campusreport.repository.TecnicoRepository;
import it.unical.campusreport.repository.TicketRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementazione di {@link AdminTicketService}.
 */
@Service
@Slf4j
public class AdminTicketServiceImpl implements AdminTicketService {

    private static final DateTimeFormatter EXPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] EXPORT_HEADERS = {
            "ID", "Titolo", "Categoria", "Stato", "Priorità", "Zona", "Cubo", "Piano",
            "Segnalante", "Tecnico", "Data Creazione", "Data Aggiornamento"
    };

    private final TicketRepository ticketRepository;
    private final TecnicoRepository tecnicoRepository;
    private final CambioStatoRepository cambioStatoRepository;
    private final EmailService emailService;

    public AdminTicketServiceImpl(TicketRepository ticketRepository,
                                  TecnicoRepository tecnicoRepository,
                                  CambioStatoRepository cambioStatoRepository,
                                  EmailService emailService) {
        this.ticketRepository = ticketRepository;
        this.tecnicoRepository = tecnicoRepository;
        this.cambioStatoRepository = cambioStatoRepository;
        this.emailService = emailService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets(AdminTicketFilter filter) {
        log.debug("Ricerca ticket admin con filtri: {}", filter);

        Specification<Ticket> spec = buildSpecification(filter);
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize());
        Page<Ticket> pagina = ticketRepository.findAll(spec, pageable);

        return pagina.getContent().stream()
                .map(t -> toTicketResponse(t, cambioStatoRepository.findByTicketOrderByTimestampAsc(t)))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public TicketResponse assegnaManualmente(UUID ticketId, AssegnazioneManualRequest request, User admin) {
        log.info("Admin {} assegna manualmente il ticket {} al tecnico {}",
                admin.getEmail(), ticketId, request.getTecnicoId());

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket non trovato con id: " + ticketId));

        Tecnico tecnico = tecnicoRepository.findById(request.getTecnicoId())
                .orElseThrow(() -> new TecnicoNotFoundException("Tecnico non trovato con id: " + request.getTecnicoId()));

        if (!tecnico.isAttivo()) {
            throw new TecnicoNonAttivoException("Il tecnico " + tecnico.getEmail() + " non è attivo");
        }

        Stato statoPrecedente = ticket.getStato();
        ticket.setTecnico(tecnico);
        ticket.setStato(Stato.ASSEGNATA);
        ticket = ticketRepository.save(ticket);

        cambioStatoRepository.save(CambioStato.builder()
                .ticket(ticket)
                .statoPrecedente(statoPrecedente)
                .statoNuovo(Stato.ASSEGNATA)
                .utente(admin)
                .nota("Assegnazione manuale da amministratore")
                .build());

        emailService.notificaTecnicoNuovaAssegnazione(ticket, tecnico);
        emailService.notificaUtenteStatoCambiato(ticket, Stato.ASSEGNATA);

        log.info("Ticket {} assegnato manualmente al tecnico {}", ticketId, tecnico.getEmail());

        List<CambioStato> storico = cambioStatoRepository.findByTicketOrderByTimestampAsc(ticket);
        return toTicketResponse(ticket, storico);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        log.info("Esportazione Excel di tutti i ticket");

        List<Ticket> tickets = ticketRepository.findAll();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Segnalazioni");
            CellStyle headerStyle = creaStileIntestazione(workbook);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(EXPORT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (Ticket t : tickets) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(t.getId().toString());
                row.createCell(1).setCellValue(nvl(t.getTitolo()));
                row.createCell(2).setCellValue(t.getCategoria() != null ? t.getCategoria().name() : "");
                row.createCell(3).setCellValue(t.getStato() != null ? t.getStato().name() : "");
                row.createCell(4).setCellValue(t.getPriorita() != null ? t.getPriorita().name() : "");
                row.createCell(5).setCellValue(t.getZona() != null ? t.getZona().getNome() : "");
                row.createCell(6).setCellValue(nvl(t.getCubo()));
                row.createCell(7).setCellValue(nvl(t.getPiano()));
                row.createCell(8).setCellValue(t.getSegnalante() != null ? t.getSegnalante().getEmail() : "");
                row.createCell(9).setCellValue(t.getTecnico() != null ? t.getTecnico().getEmail() : "");
                row.createCell(10).setCellValue(t.getCreatedAt() != null ? t.getCreatedAt().format(EXPORT_DATE_FORMAT) : "");
                row.createCell(11).setCellValue(t.getUpdatedAt() != null ? t.getUpdatedAt().format(EXPORT_DATE_FORMAT) : "");
            }

            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Errore durante la generazione del file Excel", e);
        }
    }

    private CellStyle creaStileIntestazione(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0x1F, (byte) 0x4E, (byte) 0x79}, null));
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        return style;
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }

    // ─── Helper privati ─────────────────────────────────────────────────────────

    /**
     * Costruisce la Specification con i filtri opzionali e impone l'ordinamento
     * (IN_ATTESA prima, poi priorità ALTA, poi data decrescente) direttamente
     * sulla query, ignorando la query di conteggio della paginazione.
     */
    private Specification<Ticket> buildSpecification(AdminTicketFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getStato() != null) {
                predicates.add(cb.equal(root.get("stato"), filter.getStato()));
            }
            if (filter.getCategoria() != null) {
                predicates.add(cb.equal(root.get("categoria"), filter.getCategoria()));
            }
            if (filter.getZonaId() != null) {
                predicates.add(cb.equal(root.get("zona").get("id"), filter.getZonaId()));
            }
            if (filter.getTecnicoId() != null) {
                predicates.add(cb.equal(root.get("tecnico").get("id"), filter.getTecnicoId()));
            }
            if (filter.getPriorita() != null) {
                predicates.add(cb.equal(root.get("priorita"), filter.getPriorita()));
            }
            if (StringUtils.hasText(filter.getRicerca())) {
                predicates.add(cb.like(cb.lower(root.get("titolo")), "%" + filter.getRicerca().toLowerCase() + "%"));
            }

            // La query di conteggio della paginazione ha resultType Long: l'ordinamento
            // non si applica e non ha senso applicarlo lì.
            if (query.getResultType().equals(Ticket.class)) {
                Expression<Integer> ordineStato = cb.<Integer>selectCase()
                        .when(cb.equal(root.get("stato"), Stato.IN_ATTESA), 0)
                        .otherwise(1);
                Expression<Integer> ordinePriorita = cb.<Integer>selectCase()
                        .when(cb.equal(root.get("priorita"), Priorita.ALTA), 0)
                        .otherwise(1);
                query.orderBy(cb.asc(ordineStato), cb.asc(ordinePriorita), cb.desc(root.get("createdAt")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private TicketResponse toTicketResponse(Ticket ticket, List<CambioStato> storico) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .titolo(ticket.getTitolo())
                .descrizione(ticket.getDescrizione())
                .categoria(ticket.getCategoria())
                .stato(ticket.getStato())
                .priorita(ticket.getPriorita())
                .cubo(ticket.getCubo())
                .piano(ticket.getPiano())
                .zona(ticket.getZona() != null ? toZonaResponse(ticket.getZona()) : null)
                .segnalante(toUserResponse(ticket.getSegnalante()))
                .tecnico(ticket.getTecnico() != null ? toUserResponse(ticket.getTecnico()) : null)
                .categoriaConfidenza(ticket.getCategoriaConfidenza())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .storico(storico.stream().map(this::toCambioStatoResponse).collect(Collectors.toList()))
                .build();
    }

    private CambioStatoResponse toCambioStatoResponse(CambioStato cs) {
        return CambioStatoResponse.builder()
                .id(cs.getId())
                .statoPrecedente(cs.getStatoPrecedente())
                .statoNuovo(cs.getStatoNuovo())
                .utente(toUserResponse(cs.getUtente()))
                .nota(cs.getNota())
                .timestamp(cs.getTimestamp())
                .build();
    }

    private ZonaResponse toZonaResponse(Zona zona) {
        return ZonaResponse.builder()
                .id(zona.getId())
                .nome(zona.getNome())
                .descrizione(zona.getDescrizione())
                .geojson(zona.getGeojson())
                .colore(zona.getColore())
                .build();
    }

    private UserResponse toUserResponse(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .nome(user.getNome())
                .cognome(user.getCognome())
                .email(user.getEmail())
                .ruolo(user.getRuolo())
                .build();
    }
}

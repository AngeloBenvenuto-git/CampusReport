package it.unical.campusreport;

import it.unical.campusreport.entity.CambioStato;
import it.unical.campusreport.entity.Tecnico;
import it.unical.campusreport.entity.Ticket;
import it.unical.campusreport.entity.User;
import it.unical.campusreport.entity.Zona;
import it.unical.campusreport.entity.enums.Categoria;
import it.unical.campusreport.entity.enums.Priorita;
import it.unical.campusreport.entity.enums.Ruolo;
import it.unical.campusreport.entity.enums.Stato;
import it.unical.campusreport.repository.CambioStatoRepository;
import it.unical.campusreport.repository.TecnicoRepository;
import it.unical.campusreport.repository.TicketRepository;
import it.unical.campusreport.repository.UserRepository;
import it.unical.campusreport.repository.ZonaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Popola il database con dati realistici per la demo. Gira solo con il profilo dev
 * ed è idempotente: verifica sempre il conteggio delle entità prima di inserire.
 */
@Component
@Profile("dev")
@Slf4j
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final String PASSWORD_DEMO = "password123";

    private final UserRepository userRepository;
    private final TecnicoRepository tecnicoRepository;
    private final ZonaRepository zonaRepository;
    private final TicketRepository ticketRepository;
    private final CambioStatoRepository cambioStatoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            Map<String, Zona> zoneByNome = seedZone();
            Map<String, User> userByEmail = seedUtentiETecnici();
            seedTickets(zoneByNome, userByEmail);
        } catch (Exception e) {
            log.error("DataSeeder: errore durante il popolamento del database", e);
        }
    }

    private Map<String, Zona> seedZone() {
        if (zonaRepository.count() > 0) {
            log.info("DataSeeder: zone già presenti, skip inserimento");
            return indexByNome(zonaRepository.findAll());
        }

        log.info("DataSeeder: inserimento zone...");
        List<Zona> zone = List.of(
                Zona.builder().nome("Polo Umanistico")
                        .descrizione("Cubi 0A-6C — Scienze Politiche, Economia, Biologia, Farmacia")
                        .colore("#3B82F6")
                        .geojson("{\"type\":\"Polygon\",\"coordinates\":[]}")
                        .build(),
                Zona.builder().nome("Polo Scientifico Ovest")
                        .descrizione("Cubi 7-18 — Chimica, Culture, Studi Umanistici, Istituti CNR")
                        .colore("#8B5CF6")
                        .geojson("{\"type\":\"Polygon\",\"coordinates\":[]}")
                        .build(),
                Zona.builder().nome("Biblioteca e Rettorato")
                        .descrizione("Cubi 22-25B — Biblioteche, Rettorato, CLA, Cappella")
                        .colore("#F59E0B")
                        .geojson("{\"type\":\"Polygon\",\"coordinates\":[]}")
                        .build(),
                Zona.builder().nome("Polo Scientifico Est")
                        .descrizione("Cubi 26-35 — Fisica, Matematica e Informatica, CNR Nanotec")
                        .colore("#10B981")
                        .geojson("{\"type\":\"Polygon\",\"coordinates\":[]}")
                        .build(),
                Zona.builder().nome("Polo Ingegneria")
                        .descrizione("Cubi 37-46 — Ingegneria Civile, Informatica, Meccanica, Ambiente")
                        .colore("#EF4444")
                        .geojson("{\"type\":\"Polygon\",\"coordinates\":[]}")
                        .build(),
                Zona.builder().nome("Polifunzionale")
                        .descrizione("Edificio Polifunzionale — SSSAP, Anfiteatro, Stabulario, Farmacia")
                        .colore("#EC4899")
                        .geojson("{\"type\":\"Polygon\",\"coordinates\":[]}")
                        .build(),
                Zona.builder().nome("TAU Cinema Campus")
                        .descrizione("Teatro Auditorium Unical e Cinema Campus")
                        .colore("#6366F1")
                        .geojson("{\"type\":\"Polygon\",\"coordinates\":[]}")
                        .build(),
                Zona.builder().nome("PTU e Centro Residenziale")
                        .descrizione("Piccolo Teatro Unical, Centro Residenziale, Servizi Didattici")
                        .colore("#14B8A6")
                        .geojson("{\"type\":\"Polygon\",\"coordinates\":[]}")
                        .build(),
                Zona.builder().nome("Aula Caldora")
                        .descrizione("Aula Magna U. Caldora")
                        .colore("#F97316")
                        .geojson("{\"type\":\"Polygon\",\"coordinates\":[]}")
                        .build(),
                Zona.builder().nome("Centro Sportivo")
                        .descrizione("Centro Sportivo e CAG Centro Aggregazione Giovanile")
                        .colore("#84CC16")
                        .geojson("{\"type\":\"Polygon\",\"coordinates\":[]}")
                        .build(),
                Zona.builder().nome("Polo di Innovazione")
                        .descrizione("Polo di Innovazione e Trasferimento Tecnologico, Progetto Star")
                        .colore("#06B6D4")
                        .geojson("{\"type\":\"Polygon\",\"coordinates\":[]}")
                        .build(),
                Zona.builder().nome("Centro Congressi e Aula Magna")
                        .descrizione("Centro Congressi e Aula Magna B. Andreatta")
                        .colore("#D97706")
                        .geojson("{\"type\":\"Polygon\",\"coordinates\":[]}")
                        .build()
        );

        List<Zona> salvate = zonaRepository.saveAll(zone);
        log.info("DataSeeder: {} zone inserite", salvate.size());
        return indexByNome(salvate);
    }

    private Map<String, User> seedUtentiETecnici() {
        if (userRepository.count() > 0) {
            log.info("DataSeeder: utenti già presenti, skip inserimento");
            return indexByEmail(userRepository.findAll());
        }

        log.info("DataSeeder: inserimento utenti base...");
        String passwordHash = passwordEncoder.encode(PASSWORD_DEMO);

        User marco = User.builder()
                .nome("Marco").cognome("Ferrari")
                .email("marco@studenti.unical.it")
                .passwordHash(passwordHash)
                .ruolo(Ruolo.STUDENTE)
                .attivo(true)
                .build();

        User giulia = User.builder()
                .nome("Giulia").cognome("Marino")
                .email("giulia@studenti.unical.it")
                .passwordHash(passwordHash)
                .ruolo(Ruolo.STUDENTE)
                .attivo(true)
                .build();

        User antonio = User.builder()
                .nome("Prof. Antonio").cognome("Esposito")
                .email("antonio.esposito@unical.it")
                .passwordHash(passwordHash)
                .ruolo(Ruolo.DOCENTE)
                .attivo(true)
                .build();

        User admin = User.builder()
                .nome("Admin").cognome("Sistema")
                .email("admin@unical.it")
                .passwordHash(passwordHash)
                .ruolo(Ruolo.ADMIN)
                .attivo(true)
                .build();

        userRepository.saveAll(List.of(marco, giulia, antonio, admin));
        log.info("DataSeeder: 4 utenti base inseriti");

        log.info("DataSeeder: inserimento tecnici...");
        Tecnico luca = Tecnico.builder()
                .nome("Luca").cognome("Bianchi")
                .email("luca.bianchi@unical.it")
                .passwordHash(passwordHash)
                .ruolo(Ruolo.TECNICO)
                .attivo(true)
                .specializzazioni(List.of(Categoria.ELETTRICO, Categoria.ATTREZZATURA))
                .zona("Polo Ingegneria")
                .caricoMassimo(10)
                .build();

        Tecnico sara = Tecnico.builder()
                .nome("Sara").cognome("Romano")
                .email("sara.romano@unical.it")
                .passwordHash(passwordHash)
                .ruolo(Ruolo.TECNICO)
                .attivo(true)
                .specializzazioni(List.of(Categoria.WIFI, Categoria.ALTRO))
                .zona("Polo Scientifico Ovest")
                .caricoMassimo(10)
                .build();

        Tecnico giuseppe = Tecnico.builder()
                .nome("Giuseppe").cognome("Conti")
                .email("giuseppe.conti@unical.it")
                .passwordHash(passwordHash)
                .ruolo(Ruolo.TECNICO)
                .attivo(true)
                .specializzazioni(List.of(Categoria.IDRAULICO, Categoria.ELETTRICO))
                .zona("Polo Umanistico")
                .caricoMassimo(10)
                .build();

        Tecnico elena = Tecnico.builder()
                .nome("Elena").cognome("Ricci")
                .email("elena.ricci@unical.it")
                .passwordHash(passwordHash)
                .ruolo(Ruolo.TECNICO)
                .attivo(true)
                .specializzazioni(List.of(Categoria.ATTREZZATURA, Categoria.WIFI, Categoria.ALTRO))
                .zona("Biblioteca e Rettorato")
                .caricoMassimo(10)
                .build();

        tecnicoRepository.saveAll(List.of(luca, sara, giuseppe, elena));
        log.info("DataSeeder: 4 tecnici inseriti");

        return indexByEmail(userRepository.findAll());
    }

    private void seedTickets(Map<String, Zona> zoneByNome, Map<String, User> userByEmail) {
        if (ticketRepository.count() > 0) {
            log.info("DataSeeder: segnalazioni già presenti, skip inserimento");
            return;
        }

        log.info("DataSeeder: inserimento segnalazioni di esempio...");

        User marco = userByEmail.get("marco@studenti.unical.it");
        User giulia = userByEmail.get("giulia@studenti.unical.it");
        User antonio = userByEmail.get("antonio.esposito@unical.it");
        User luca = userByEmail.get("luca.bianchi@unical.it");
        User sara = userByEmail.get("sara.romano@unical.it");
        User giuseppe = userByEmail.get("giuseppe.conti@unical.it");
        User elena = userByEmail.get("elena.ricci@unical.it");

        creaTicketCompletato(
                "Proiettore non funzionante",
                "Il proiettore dell'aula 15B non si accende, impossibile tenere lezioni multimediali",
                Categoria.ATTREZZATURA, zoneByNome.get("Polo Ingegneria"), "15B", "Piano Terra",
                marco, luca, Priorita.NORMALE, 10, 8);

        creaTicketInLavorazione(
                "WiFi assente in tutto il piano",
                "Da questa mattina il WiFi non funziona in nessuna aula del secondo piano",
                Categoria.WIFI, zoneByNome.get("Polo Scientifico Ovest"), "14C", "2° Piano",
                antonio, sara, Priorita.ALTA, 3);

        creaTicketAssegnata(
                "Presa elettrica danneggiata",
                "La presa elettrica vicino alla cattedra produce scintille, pericolo sicurezza",
                Categoria.ELETTRICO, zoneByNome.get("Polo Umanistico"), "3C", "1° Piano",
                giulia, giuseppe, Priorita.NORMALE, 1);

        creaTicketInAttesa(
                "Perdita d'acqua nel bagno",
                "Nel bagno del piano terra c'è una perdita d'acqua dal lavandino, pavimento bagnato",
                Categoria.IDRAULICO, zoneByNome.get("Biblioteca e Rettorato"), "25B", "Piano Terra",
                marco, Priorita.NORMALE, 0);

        creaTicketCompletato(
                "Aria condizionata bloccata al massimo",
                "L'aria condizionata dell'aula magna è bloccata al massimo del freddo, impossibile lavorare",
                Categoria.ATTREZZATURA, zoneByNome.get("Centro Congressi e Aula Magna"), "Aula Magna", "Piano Terra",
                antonio, elena, Priorita.ALTA, 15, 13);

        creaTicketAssegnata(
                "Monitor PC non funzionante",
                "Il monitor della postazione numero 7 non si accende",
                Categoria.ATTREZZATURA, zoneByNome.get("Polo Scientifico Est"), "31C", "1° Piano",
                giulia, elena, Priorita.NORMALE, 2);

        log.info("DataSeeder: 6 segnalazioni inserite");
    }

    private void creaTicketCompletato(String titolo, String descrizione, Categoria categoria, Zona zona,
                                         String cubo, String piano, User segnalante, User tecnico,
                                         Priorita priorita, int giorniFaCreazione, int giorniFaCompletamento) {
        LocalDateTime creazione = LocalDateTime.now().minusDays(giorniFaCreazione);
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .titolo(titolo).descrizione(descrizione).categoria(categoria)
                .stato(Stato.COMPLETATA).priorita(priorita)
                .cubo(cubo).piano(piano)
                .zona(zona).segnalante(segnalante).tecnico(tecnico)
                .categoriaConfidenza(0.9f)
                .build());
        backdataCreazioneTicket(ticket, creazione);

        LocalDateTime completamento = LocalDateTime.now().minusDays(giorniFaCompletamento);
        salvaTransizione(ticket, null, Stato.APERTA, segnalante, creazione);
        salvaTransizione(ticket, Stato.APERTA, Stato.ASSEGNATA, null, creazione.plusHours(1));
        salvaTransizione(ticket, Stato.ASSEGNATA, Stato.IN_LAVORAZIONE, tecnico, creazione.plusHours(2));
        salvaTransizione(ticket, Stato.IN_LAVORAZIONE, Stato.COMPLETATA, tecnico, completamento);
    }

    private void creaTicketInLavorazione(String titolo, String descrizione, Categoria categoria, Zona zona,
                                            String cubo, String piano, User segnalante, User tecnico,
                                            Priorita priorita, int giorniFaCreazione) {
        LocalDateTime creazione = LocalDateTime.now().minusDays(giorniFaCreazione);
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .titolo(titolo).descrizione(descrizione).categoria(categoria)
                .stato(Stato.IN_LAVORAZIONE).priorita(priorita)
                .cubo(cubo).piano(piano)
                .zona(zona).segnalante(segnalante).tecnico(tecnico)
                .categoriaConfidenza(0.85f)
                .build());
        backdataCreazioneTicket(ticket, creazione);

        salvaTransizione(ticket, null, Stato.APERTA, segnalante, creazione);
        salvaTransizione(ticket, Stato.APERTA, Stato.ASSEGNATA, null, creazione.plusHours(1));
        salvaTransizione(ticket, Stato.ASSEGNATA, Stato.IN_LAVORAZIONE, tecnico, creazione.plusHours(4));
    }

    private void creaTicketAssegnata(String titolo, String descrizione, Categoria categoria, Zona zona,
                                        String cubo, String piano, User segnalante, User tecnico,
                                        Priorita priorita, int giorniFaCreazione) {
        LocalDateTime creazione = LocalDateTime.now().minusDays(giorniFaCreazione);
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .titolo(titolo).descrizione(descrizione).categoria(categoria)
                .stato(Stato.ASSEGNATA).priorita(priorita)
                .cubo(cubo).piano(piano)
                .zona(zona).segnalante(segnalante).tecnico(tecnico)
                .categoriaConfidenza(0.8f)
                .build());
        backdataCreazioneTicket(ticket, creazione);

        salvaTransizione(ticket, null, Stato.APERTA, segnalante, creazione);
        salvaTransizione(ticket, Stato.APERTA, Stato.ASSEGNATA, null, creazione.plusHours(1));
    }

    private void creaTicketInAttesa(String titolo, String descrizione, Categoria categoria, Zona zona,
                                       String cubo, String piano, User segnalante,
                                       Priorita priorita, int giorniFaCreazione) {
        LocalDateTime creazione = LocalDateTime.now().minusDays(giorniFaCreazione);
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .titolo(titolo).descrizione(descrizione).categoria(categoria)
                .stato(Stato.IN_ATTESA).priorita(priorita)
                .cubo(cubo).piano(piano)
                .zona(zona).segnalante(segnalante).tecnico(null)
                .categoriaConfidenza(0.75f)
                .build());
        backdataCreazioneTicket(ticket, creazione);

        salvaTransizione(ticket, null, Stato.APERTA, segnalante, creazione);
        salvaTransizione(ticket, Stato.APERTA, Stato.IN_ATTESA, null, creazione.plusHours(1));
    }

    /**
     * Ticket.createdAt è @CreationTimestamp(updatable = false): Hibernate impone "now" all'insert
     * e ignora qualunque valore assegnato manualmente all'entità, quindi la data realistica va
     * scritta con un UPDATE nativo che bypassa il flag updatable.
     */
    private void backdataCreazioneTicket(Ticket ticket, LocalDateTime creazione) {
        jdbcTemplate.update("UPDATE tickets SET created_at = ? WHERE id = ?", creazione, ticket.getId());
    }

    private void salvaTransizione(Ticket ticket, Stato precedente, Stato nuovo, User utente, LocalDateTime timestamp) {
        CambioStato cambioStato = CambioStato.builder()
                .ticket(ticket)
                .statoPrecedente(precedente)
                .statoNuovo(nuovo)
                .utente(utente)
                .build();
        CambioStato salvato = cambioStatoRepository.save(cambioStato);
        // CambioStato.timestamp è @CreationTimestamp(updatable = false): stesso motivo di backdataCreazioneTicket.
        jdbcTemplate.update("UPDATE cambio_stato SET \"timestamp\" = ? WHERE id = ?", timestamp, salvato.getId());
    }

    private Map<String, Zona> indexByNome(List<Zona> zone) {
        return zone.stream().collect(java.util.stream.Collectors.toMap(Zona::getNome, z -> z, (a, b) -> a));
    }

    private Map<String, User> indexByEmail(List<User> utenti) {
        return utenti.stream().collect(java.util.stream.Collectors.toMap(User::getEmail, u -> u, (a, b) -> a));
    }
}

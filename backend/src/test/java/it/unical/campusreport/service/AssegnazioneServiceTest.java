package it.unical.campusreport.service;

import it.unical.campusreport.config.AssegnazioneConfig;
import it.unical.campusreport.entity.Tecnico;
import it.unical.campusreport.entity.Ticket;
import it.unical.campusreport.entity.Zona;
import it.unical.campusreport.entity.enums.Categoria;
import it.unical.campusreport.entity.enums.Priorita;
import it.unical.campusreport.entity.enums.Ruolo;
import it.unical.campusreport.entity.enums.Stato;
import it.unical.campusreport.repository.CambioStatoRepository;
import it.unical.campusreport.repository.TecnicoRepository;
import it.unical.campusreport.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssegnazioneServiceTest {

    @Mock private TecnicoRepository tecnicoRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private CambioStatoRepository cambioStatoRepository;
    @Mock private EmailService emailService;

    private AssegnazioneServiceImpl service;
    private AssegnazioneConfig config;

    @BeforeEach
    void setUp() {
        config = new AssegnazioneConfig();
        config.setAlpha(0.5);
        config.setBeta(0.3);
        config.setGamma(0.1);
        config.setDelta(0.1);
        service = new AssegnazioneServiceImpl(
                tecnicoRepository, ticketRepository, cambioStatoRepository, config, emailService,
                new MatriceAdiacenzaService());
    }

    // ─── Fixture helpers ────────────────────────────────────────────────────────

    private Tecnico buildTecnico(int caricoMassimo, Categoria... specializzazioni) {
        return Tecnico.builder()
                .id(UUID.randomUUID())
                .nome("Mario")
                .cognome("Rossi")
                .email("tecnico+" + UUID.randomUUID() + "@test.it")
                .passwordHash("hash")
                .ruolo(Ruolo.TECNICO)
                .attivo(true)
                .specializzazioni(List.of(specializzazioni))
                .caricoMassimo(caricoMassimo)
                .build();
    }

    private Ticket buildTicket(Categoria categoria) {
        return Ticket.builder()
                .id(UUID.randomUUID())
                .titolo("Test")
                .descrizione("Descrizione test")
                .categoria(categoria)
                .stato(Stato.APERTA)
                .priorita(Priorita.NORMALE)
                .build();
    }

    // ─── Test 1: Caso base ───────────────────────────────────────────────────────

    @Test
    void assegna_conTecnicoDisponibile_assegnaEImpostaStatoASSEGNATA() {
        Tecnico tecnico = buildTecnico(10, Categoria.WIFI);
        Ticket ticket = buildTicket(Categoria.WIFI);

        when(tecnicoRepository.findAttiviBySpecializzazioneWithLock(Categoria.WIFI))
                .thenReturn(List.of(tecnico));
        when(ticketRepository.countByTecnicoAndStatoIn(eq(tecnico), anyList()))
                .thenReturn(0L);

        service.assegna(ticket);

        assertThat(ticket.getStato()).isEqualTo(Stato.ASSEGNATA);
        assertThat(ticket.getTecnico()).isEqualTo(tecnico);
        verify(cambioStatoRepository).save(any());
        verify(emailService).notificaTecnicoNuovaAssegnazione(ticket, tecnico);
        verify(emailService).notificaUtenteStatoCambiato(ticket, Stato.ASSEGNATA);
    }

    // ─── Test 2: Carico pieno ────────────────────────────────────────────────────

    @Test
    void assegna_conTecnicoACaricoMassimo_impostatoIN_ATTESA() {
        Tecnico tecnico = buildTecnico(10, Categoria.WIFI);
        Ticket ticket = buildTicket(Categoria.WIFI);

        when(tecnicoRepository.findAttiviBySpecializzazioneWithLock(Categoria.WIFI))
                .thenReturn(List.of(tecnico));
        // carico == massimo: 10/10 → escluso
        when(ticketRepository.countByTecnicoAndStatoIn(eq(tecnico), anyList()))
                .thenReturn(10L);

        service.assegna(ticket);

        assertThat(ticket.getStato()).isEqualTo(Stato.IN_ATTESA);
        assertThat(ticket.getTecnico()).isNull();
        verify(cambioStatoRepository).save(any());
        verify(emailService).notificaAdminTicketInAttesa(ticket);
    }

    // ─── Test 3: Nessun tecnico disponibile ─────────────────────────────────────

    @Test
    void assegna_senzaTecnici_impostatoIN_ATTESA() {
        Ticket ticket = buildTicket(Categoria.ELETTRICO);

        when(tecnicoRepository.findAttiviBySpecializzazioneWithLock(Categoria.ELETTRICO))
                .thenReturn(List.of());

        service.assegna(ticket);

        assertThat(ticket.getStato()).isEqualTo(Stato.IN_ATTESA);
        assertThat(ticket.getTecnico()).isNull();
        verify(cambioStatoRepository).save(any());
        verify(emailService).notificaAdminTicketInAttesa(ticket);
    }

    // ─── Test 4: Riassegnazione con tecnico escluso ──────────────────────────────

    @Test
    void riassegna_escludeTecnicoRifiutante_assegnaAlSecondo() {
        Tecnico tecnico1 = buildTecnico(10, Categoria.IDRAULICO);
        Tecnico tecnico2 = buildTecnico(10, Categoria.IDRAULICO);
        Ticket ticket = buildTicket(Categoria.IDRAULICO);
        ticket.setStato(Stato.RIFIUTATA);

        when(tecnicoRepository.findAttiviBySpecializzazioneWithLock(Categoria.IDRAULICO))
                .thenReturn(List.of(tecnico1, tecnico2));
        // tecnico1 è escluso prima del check sul carico; stub solo per tecnico2
        when(ticketRepository.countByTecnicoAndStatoIn(eq(tecnico2), anyList())).thenReturn(5L);

        service.riassegna(ticket, tecnico1.getId());

        assertThat(ticket.getStato()).isEqualTo(Stato.ASSEGNATA);
        assertThat(ticket.getTecnico()).isEqualTo(tecnico2);
        verify(emailService).notificaTecnicoNuovaAssegnazione(ticket, tecnico2);
        verify(emailService).notificaUtenteStatoCambiato(ticket, Stato.ASSEGNATA);
    }

    // ─── Test 5: Selezione per score massimo ─────────────────────────────────────

    @Test
    void assegna_traDueTecnici_scegliQuelloConScoreMaggiore() {
        // a parità di urgenza e zona, vince il tecnico meno carico:
        // tecnicoA: carico 0/10 → contributo β = 0.3*(1-0)   = 0.30
        // tecnicoB: carico 5/10 → contributo β = 0.3*(1-0.5) = 0.15
        Tecnico tecnicoA = buildTecnico(10, Categoria.ELETTRICO);
        Tecnico tecnicoB = buildTecnico(10, Categoria.ELETTRICO);
        Ticket ticket = buildTicket(Categoria.ELETTRICO);

        when(tecnicoRepository.findAttiviBySpecializzazioneWithLock(Categoria.ELETTRICO))
                .thenReturn(List.of(tecnicoA, tecnicoB));
        when(ticketRepository.countByTecnicoAndStatoIn(eq(tecnicoA), anyList())).thenReturn(0L);
        when(ticketRepository.countByTecnicoAndStatoIn(eq(tecnicoB), anyList())).thenReturn(5L);

        service.assegna(ticket);

        assertThat(ticket.getStato()).isEqualTo(Stato.ASSEGNATA);
        assertThat(ticket.getTecnico()).isEqualTo(tecnicoA);
    }

    // ─── Test 6: Urgenza ─────────────────────────────────────────────────────────

    @Test
    void score_urgenzaAlta_aumentaPunteggio() {
        Tecnico tecnico = buildTecnico(10, Categoria.WIFI);
        Ticket urgente = buildTicket(Categoria.WIFI);
        urgente.setUrgenza(5);
        Ticket nonUrgente = buildTicket(Categoria.WIFI);
        nonUrgente.setUrgenza(1);

        when(ticketRepository.countByTecnicoAndStatoIn(eq(tecnico), anyList())).thenReturn(2L);

        double scoreUrgente = service.calcolaScore(tecnico, urgente);
        double scoreNonUrgente = service.calcolaScore(tecnico, nonUrgente);

        assertThat(scoreUrgente).isGreaterThan(scoreNonUrgente);
        // urgenza normalizzata: 5 → 1.0, 1 → 0.0 → differenza = γ
        assertThat(scoreUrgente - scoreNonUrgente).isCloseTo(config.getGamma(), within(1e-9));
    }

    // ─── Test 7: Stessa zona ─────────────────────────────────────────────────────

    @Test
    void score_stessaZona_aumentaPunteggio() {
        Tecnico vicino = buildTecnico(10, Categoria.ELETTRICO);
        vicino.setZona("Polo Ingegneria");
        Tecnico lontano = buildTecnico(10, Categoria.ELETTRICO);
        lontano.setZona("Centro Sportivo");
        Ticket ticket = buildTicket(Categoria.ELETTRICO);
        ticket.setZona(Zona.builder().nome("Polo Ingegneria").build());

        when(ticketRepository.countByTecnicoAndStatoIn(any(Tecnico.class), anyList())).thenReturn(3L);

        double scoreVicino = service.calcolaScore(vicino, ticket);
        double scoreLontano = service.calcolaScore(lontano, ticket);

        assertThat(scoreVicino).isGreaterThan(scoreLontano);
        // vicinanza: 1.0 vs 0.1 → differenza = δ·0.9
        assertThat(scoreVicino - scoreLontano).isCloseTo(config.getDelta() * 0.9, within(1e-9));
    }

    // ─── Test 8: Zona adiacente (0.7) ────────────────────────────────────────────

    @Test
    void score_zonaAdiacente_punteggio07() {
        Tecnico stessaZona = buildTecnico(10, Categoria.IDRAULICO);
        stessaZona.setZona("Polo Scientifico Est");
        Tecnico adiacente = buildTecnico(10, Categoria.IDRAULICO);
        adiacente.setZona("Polo Ingegneria");
        Tecnico lontano = buildTecnico(10, Categoria.IDRAULICO);
        lontano.setZona("Centro Sportivo");
        Ticket ticket = buildTicket(Categoria.IDRAULICO);
        ticket.setUrgenza(3);
        ticket.setZona(Zona.builder().nome("Polo Scientifico Est").build());

        when(ticketRepository.countByTecnicoAndStatoIn(any(Tecnico.class), anyList())).thenReturn(0L);

        double scoreStessa = service.calcolaScore(stessaZona, ticket);
        double scoreAdiacente = service.calcolaScore(adiacente, ticket);
        double scoreLontano = service.calcolaScore(lontano, ticket);

        // score = 0.5·1 + 0.3·1 + 0.1·0.5 + 0.1·0.7 = 0.92
        assertThat(scoreAdiacente).isCloseTo(0.92, within(1e-9));
        assertThat(scoreStessa - scoreAdiacente).isCloseTo(config.getDelta() * 0.3, within(1e-9));
        assertThat(scoreAdiacente - scoreLontano).isCloseTo(config.getDelta() * 0.6, within(1e-9));
    }

    // ─── Test 9: Matrice simmetrica ──────────────────────────────────────────────

    @Test
    void matriceAdiacenza_simmetricaEFallback() {
        MatriceAdiacenzaService matrice = new MatriceAdiacenzaService();

        assertThat(matrice.calcolaVicinanza("Polo Ingegneria", "TAU Cinema Campus")).isEqualTo(0.7);
        assertThat(matrice.calcolaVicinanza("TAU Cinema Campus", "Polo Ingegneria")).isEqualTo(0.7);
        assertThat(matrice.calcolaVicinanza("Polo di Innovazione", "Polo Ingegneria")).isEqualTo(0.4);
        assertThat(matrice.calcolaVicinanza("Centro Sportivo", "Polo Ingegneria")).isEqualTo(0.1);
        assertThat(matrice.calcolaVicinanza("Aula Caldora", "Aula Caldora")).isEqualTo(1.0);
        assertThat(matrice.calcolaVicinanza("", "Aula Caldora")).isEqualTo(0.1);
    }
}

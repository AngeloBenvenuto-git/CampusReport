package it.unical.campusreport.service;

import it.unical.campusreport.config.AssegnazioneConfig;
import it.unical.campusreport.entity.Tecnico;
import it.unical.campusreport.entity.Ticket;
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

    private AssegnazioneServiceImpl service;

    @BeforeEach
    void setUp() {
        AssegnazioneConfig config = new AssegnazioneConfig();
        config.setAlpha(0.6);
        config.setBeta(0.4);
        service = new AssegnazioneServiceImpl(
                tecnicoRepository, ticketRepository, cambioStatoRepository, config);
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
    }

    // ─── Test 5: Selezione per score massimo ─────────────────────────────────────

    @Test
    void assegna_traDueTecnici_scegliQuelloConScoreMaggiore() {
        // tecnicoA: carico 0/10 → score = 0.6*1 + 0.4*(1-0/10) = 1.0
        // tecnicoB: carico 5/10 → score = 0.6*1 + 0.4*(1-0.5)  = 0.8
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
}

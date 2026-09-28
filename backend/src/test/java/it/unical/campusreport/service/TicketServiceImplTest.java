package it.unical.campusreport.service;

import it.unical.campusreport.dto.ModificaTicketRequest;
import it.unical.campusreport.dto.NlpResponse;
import it.unical.campusreport.dto.NlpUrgenzaResponse;
import it.unical.campusreport.dto.RifiutoRequest;
import it.unical.campusreport.dto.TicketRequest;
import it.unical.campusreport.dto.TicketResponse;
import it.unical.campusreport.entity.Allegato;
import it.unical.campusreport.entity.CambioStato;
import it.unical.campusreport.entity.Ticket;
import it.unical.campusreport.entity.User;
import it.unical.campusreport.entity.Zona;
import it.unical.campusreport.entity.enums.Categoria;
import it.unical.campusreport.entity.enums.Priorita;
import it.unical.campusreport.entity.enums.Ruolo;
import it.unical.campusreport.entity.enums.Stato;
import it.unical.campusreport.entity.enums.TipoRifiuto;
import it.unical.campusreport.exception.TicketNonModificabileException;
import it.unical.campusreport.exception.UnauthorizedTicketAccessException;
import it.unical.campusreport.repository.AllegatoRepository;
import it.unical.campusreport.repository.CambioStatoRepository;
import it.unical.campusreport.repository.TicketRepository;
import it.unical.campusreport.repository.ZonaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private ZonaRepository zonaRepository;
    @Mock private CambioStatoRepository cambioStatoRepository;
    @Mock private AllegatoRepository allegatoRepository;
    @Mock private NlpClient nlpClient;
    @Mock private AssegnazioneService assegnazioneService;
    @Mock private EmailService emailService;

    private TicketServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TicketServiceImpl(
                ticketRepository, zonaRepository, cambioStatoRepository, allegatoRepository,
                nlpClient, assegnazioneService, emailService);
    }

    private User buildUser(String nome, Ruolo ruolo) {
        return User.builder()
                .id(UUID.randomUUID())
                .nome(nome)
                .cognome("Rossi")
                .email(nome.toLowerCase() + "@test.it")
                .passwordHash("hash")
                .ruolo(ruolo)
                .attivo(true)
                .build();
    }

    private Ticket buildTicket(User segnalante, User tecnico) {
        return Ticket.builder()
                .id(UUID.randomUUID())
                .titolo("WiFi assente")
                .descrizione("Il WiFi non funziona")
                .categoria(Categoria.WIFI)
                .stato(Stato.ASSEGNATA)
                .priorita(Priorita.NORMALE)
                .zona(Zona.builder().id(UUID.randomUUID()).nome("Cubo 42").build())
                .segnalante(segnalante)
                .tecnico(tecnico)
                .build();
    }

    private RifiutoRequest buildRequest(TipoRifiuto tipo, String motivazione) {
        RifiutoRequest request = new RifiutoRequest();
        request.setTipoRifiuto(tipo);
        request.setMotivazione(motivazione);
        return request;
    }

    // ─── creaTicket: priorità derivata dall'urgenza ──────────────────────────────

    private Ticket creaTicketConUrgenza(User utente, Integer urgenzaUtente, int urgenzaNlp) {
        Zona zona = Zona.builder().id(UUID.randomUUID()).nome("Cubo 42").build();

        TicketRequest request = new TicketRequest();
        request.setZonaId(zona.getId());
        request.setTitolo("WiFi assente");
        request.setDescrizione("Il WiFi non funziona");
        request.setCategoria(Categoria.WIFI);
        request.setUrgenza(urgenzaUtente);

        NlpResponse nlpResponse = new NlpResponse();
        nlpResponse.setCategoria("WIFI");
        nlpResponse.setConfidenza(0.9f);
        NlpUrgenzaResponse urgenzaResponse = new NlpUrgenzaResponse();
        urgenzaResponse.setUrgenza(urgenzaNlp);

        when(zonaRepository.findById(zona.getId())).thenReturn(Optional.of(zona));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(nlpClient.classifyText(any())).thenReturn(nlpResponse);
        when(nlpClient.classifyUrgenza(any())).thenReturn(urgenzaResponse);
        when(cambioStatoRepository.findByTicketOrderByTimestampAsc(any())).thenReturn(List.of());

        service.creaTicket(request, utente);

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(assegnazioneService).assegna(captor.capture());
        return captor.getValue();
    }

    @Test
    void creaTicket_docenteConUrgenza3_prioritaNormale() {
        Ticket ticket = creaTicketConUrgenza(buildUser("Paolo", Ruolo.DOCENTE), 3, 5);

        assertThat(ticket.getUrgenza()).isEqualTo(3);
        assertThat(ticket.getPriorita()).isEqualTo(Priorita.NORMALE);
    }

    @Test
    void creaTicket_urgenza4_prioritaAlta() {
        Ticket ticket = creaTicketConUrgenza(buildUser("Luca", Ruolo.STUDENTE), 4, 1);

        assertThat(ticket.getPriorita()).isEqualTo(Priorita.ALTA);
    }

    @Test
    void creaTicket_urgenza3_prioritaNormale() {
        Ticket ticket = creaTicketConUrgenza(buildUser("Luca", Ruolo.STUDENTE), 3, 5);

        assertThat(ticket.getPriorita()).isEqualTo(Priorita.NORMALE);
    }

    @Test
    void creaTicket_urgenzaStimataDaNlp_determinaPriorita() {
        Ticket ticket = creaTicketConUrgenza(buildUser("Luca", Ruolo.STUDENTE), null, 4);

        assertThat(ticket.getUrgenza()).isEqualTo(4);
        assertThat(ticket.getPriorita()).isEqualTo(Priorita.ALTA);
    }

    // ─── CASO 2: RIASSEGNA ────────────────────────────────────────────────────────

    @Test
    void rifiutaTicket_riassegna_riassegnaAAltroTecnicoERestituisceIlTicket() {
        User segnalante = buildUser("Luca", Ruolo.STUDENTE);
        User tecnico = buildUser("Mario", Ruolo.TECNICO);
        Ticket ticket = buildTicket(segnalante, tecnico);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cambioStatoRepository.findByTicketOrderByTimestampAsc(ticket)).thenReturn(List.of());

        RifiutoRequest request = buildRequest(TipoRifiuto.RIASSEGNA, "Non ho le competenze necessarie");
        TicketResponse response = service.rifiutaTicket(ticket.getId(), request, tecnico);

        assertThat(response).isNotNull();
        assertThat(ticket.getTecnico()).isNull();
        verify(assegnazioneService).riassegna(ticket, tecnico.getId());
        verify(emailService, never()).notificaRifiutoDefinitivo(any(), any());
        verify(ticketRepository, never()).delete(any(Ticket.class));

        ArgumentCaptor<CambioStato> captor = ArgumentCaptor.forClass(CambioStato.class);
        verify(cambioStatoRepository).save(captor.capture());
        assertThat(captor.getValue().getStatoNuovo()).isEqualTo(Stato.RIFIUTATA);
        assertThat(captor.getValue().getNota()).isEqualTo("Non ho le competenze necessarie");
    }

    // ─── CASO 3: ELIMINA ──────────────────────────────────────────────────────────

    @Test
    void rifiutaTicket_elimina_notificaSegnalanteEdEliminaTicketRestituendoNull() {
        User segnalante = buildUser("Luca", Ruolo.STUDENTE);
        User tecnico = buildUser("Mario", Ruolo.TECNICO);
        Ticket ticket = buildTicket(segnalante, tecnico);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(allegatoRepository.findByTicket(ticket)).thenReturn(List.of());
        when(cambioStatoRepository.findByTicketOrderByTimestampAsc(ticket))
                .thenReturn(List.of(CambioStato.builder().id(UUID.randomUUID()).ticket(ticket).build()));

        RifiutoRequest request = buildRequest(TipoRifiuto.ELIMINA, "Segnalazione non pertinente");
        TicketResponse response = service.rifiutaTicket(ticket.getId(), request, tecnico);

        assertThat(response).isNull();
        verify(emailService).notificaRifiutoDefinitivo(ticket, "Segnalazione non pertinente");
        verify(ticketRepository).delete(any(Ticket.class));
        verify(cambioStatoRepository).deleteAll(anyList());
        verify(allegatoRepository).deleteAll(anyList());
        verify(assegnazioneService, never()).riassegna(any(), any());

        ArgumentCaptor<CambioStato> captor = ArgumentCaptor.forClass(CambioStato.class);
        verify(cambioStatoRepository).save(captor.capture());
        assertThat(captor.getValue().getNota()).isEqualTo("ELIMINATA: Segnalazione non pertinente");
    }

    @Test
    void rifiutaTicket_elimina_rimuoveAllegatiDalFilesystemPrimaDiEliminarli() {
        User segnalante = buildUser("Luca", Ruolo.STUDENTE);
        User tecnico = buildUser("Mario", Ruolo.TECNICO);
        Ticket ticket = buildTicket(segnalante, tecnico);
        Allegato allegato = Allegato.builder()
                .id(UUID.randomUUID())
                .ticket(ticket)
                .filename("foto.jpg")
                .path("percorso/inesistente/foto.jpg")
                .mimetype("image/jpeg")
                .dimensione(100)
                .build();

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(allegatoRepository.findByTicket(ticket)).thenReturn(List.of(allegato));
        when(cambioStatoRepository.findByTicketOrderByTimestampAsc(ticket)).thenReturn(List.of());

        RifiutoRequest request = buildRequest(TipoRifiuto.ELIMINA, "Segnalazione non pertinente");
        service.rifiutaTicket(ticket.getId(), request, tecnico);

        verify(allegatoRepository, times(2)).findByTicket(ticket);
        verify(allegatoRepository).deleteAll(List.of(allegato));
    }

    // ─── Autorizzazione ─────────────────────────────────────────────────────────

    @Test
    void rifiutaTicket_ticketNonAssegnatoAlTecnico_lanciaUnauthorized() {
        User segnalante = buildUser("Luca", Ruolo.STUDENTE);
        User tecnico = buildUser("Mario", Ruolo.TECNICO);
        User altroTecnico = buildUser("Anna", Ruolo.TECNICO);
        Ticket ticket = buildTicket(segnalante, altroTecnico);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        RifiutoRequest request = buildRequest(TipoRifiuto.RIASSEGNA, "Non ho le competenze necessarie");

        assertThatThrownBy(() -> service.rifiutaTicket(ticket.getId(), request, tecnico))
                .isInstanceOf(UnauthorizedTicketAccessException.class);
    }

    // ─── modificaTicket ─────────────────────────────────────────────────────────

    private ModificaTicketRequest buildModificaRequest(String titolo, String descrizione,
                                                        Categoria categoria, String cubo, String piano) {
        ModificaTicketRequest request = new ModificaTicketRequest();
        request.setTitolo(titolo);
        request.setDescrizione(descrizione);
        request.setCategoria(categoria);
        request.setCubo(cubo);
        request.setPiano(piano);
        return request;
    }

    @Test
    void modificaTicket_statoAperta_aggiornaCampiERegistraCambioStatoConStessoStato() {
        User segnalante = buildUser("Luca", Ruolo.STUDENTE);
        Ticket ticket = buildTicket(segnalante, null);
        ticket.setStato(Stato.APERTA);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cambioStatoRepository.findByTicketOrderByTimestampAsc(ticket)).thenReturn(List.of());

        ModificaTicketRequest request = buildModificaRequest(
                "WiFi assente in aula", "Descrizione aggiornata", Categoria.WIFI, "12A", "1° Piano");

        TicketResponse response = service.modificaTicket(ticket.getId(), request, segnalante);

        assertThat(response).isNotNull();
        assertThat(ticket.getTitolo()).isEqualTo("WiFi assente in aula");
        assertThat(ticket.getDescrizione()).isEqualTo("Descrizione aggiornata");
        assertThat(ticket.getCubo()).isEqualTo("12A");
        assertThat(ticket.getPiano()).isEqualTo("1° Piano");
        verify(emailService, never())
                .notificaTecnicoModificaSegnalazione(any(), any(), any(), any(), any(), any());
        verify(nlpClient, never()).classifyText(any());

        ArgumentCaptor<CambioStato> captor = ArgumentCaptor.forClass(CambioStato.class);
        verify(cambioStatoRepository).save(captor.capture());
        assertThat(captor.getValue().getStatoPrecedente()).isEqualTo(Stato.APERTA);
        assertThat(captor.getValue().getStatoNuovo()).isEqualTo(Stato.APERTA);
        assertThat(captor.getValue().getNota()).isEqualTo("Segnalazione modificata dall'utente");
    }

    @Test
    void modificaTicket_statoAssegnataConCambioCategoria_ricalcolaConfidenzaEInvioEmailAlTecnico() {
        User segnalante = buildUser("Luca", Ruolo.STUDENTE);
        User tecnico = buildUser("Mario", Ruolo.TECNICO);
        Ticket ticket = buildTicket(segnalante, tecnico);
        ticket.setStato(Stato.ASSEGNATA);
        ticket.setCategoria(Categoria.WIFI);

        NlpResponse nlpResponse = new NlpResponse();
        nlpResponse.setCategoria("ELETTRICO");
        nlpResponse.setConfidenza(0.92f);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(nlpClient.classifyText(any())).thenReturn(nlpResponse);
        when(cambioStatoRepository.findByTicketOrderByTimestampAsc(ticket)).thenReturn(List.of());

        ModificaTicketRequest request = buildModificaRequest(
                "Presa rotta", "In realtà è un problema elettrico", Categoria.ELETTRICO, null, null);

        service.modificaTicket(ticket.getId(), request, segnalante);

        assertThat(ticket.getCategoria()).isEqualTo(Categoria.ELETTRICO);
        assertThat(ticket.getCategoriaConfidenza()).isEqualTo(0.92f);
        verify(nlpClient).classifyText("In realtà è un problema elettrico");
        // Il tecnico assegnato non viene ricalcolato
        assertThat(ticket.getTecnico()).isEqualTo(tecnico);
        verify(assegnazioneService, never()).assegna(any());
        verify(emailService).notificaTecnicoModificaSegnalazione(
                ticket, "WiFi assente", "Il WiFi non funziona", Categoria.WIFI, null, null);
    }

    @Test
    void modificaTicket_utenteNonSegnalante_lanciaUnauthorized() {
        User segnalante = buildUser("Luca", Ruolo.STUDENTE);
        User altroUtente = buildUser("Anna", Ruolo.STUDENTE);
        Ticket ticket = buildTicket(segnalante, null);
        ticket.setStato(Stato.APERTA);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        ModificaTicketRequest request = buildModificaRequest("Titolo", "Descrizione", Categoria.WIFI, null, null);

        assertThatThrownBy(() -> service.modificaTicket(ticket.getId(), request, altroUtente))
                .isInstanceOf(UnauthorizedTicketAccessException.class);
    }

    @Test
    void modificaTicket_statoInLavorazione_lanciaTicketNonModificabile() {
        User segnalante = buildUser("Luca", Ruolo.STUDENTE);
        User tecnico = buildUser("Mario", Ruolo.TECNICO);
        Ticket ticket = buildTicket(segnalante, tecnico);
        ticket.setStato(Stato.IN_LAVORAZIONE);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        ModificaTicketRequest request = buildModificaRequest("Titolo", "Descrizione", Categoria.WIFI, null, null);

        assertThatThrownBy(() -> service.modificaTicket(ticket.getId(), request, segnalante))
                .isInstanceOf(TicketNonModificabileException.class);
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void modificaTicket_statoCompletata_lanciaTicketNonModificabile() {
        User segnalante = buildUser("Luca", Ruolo.STUDENTE);
        User tecnico = buildUser("Mario", Ruolo.TECNICO);
        Ticket ticket = buildTicket(segnalante, tecnico);
        ticket.setStato(Stato.COMPLETATA);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        ModificaTicketRequest request = buildModificaRequest("Titolo", "Descrizione", Categoria.WIFI, null, null);

        assertThatThrownBy(() -> service.modificaTicket(ticket.getId(), request, segnalante))
                .isInstanceOf(TicketNonModificabileException.class);
    }
}

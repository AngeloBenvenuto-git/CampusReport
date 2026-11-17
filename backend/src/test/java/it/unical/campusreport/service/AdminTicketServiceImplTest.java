package it.unical.campusreport.service;

import it.unical.campusreport.dto.AdminModificaTicketRequest;
import it.unical.campusreport.dto.TicketResponse;
import it.unical.campusreport.entity.CambioStato;
import it.unical.campusreport.entity.Ticket;
import it.unical.campusreport.entity.User;
import it.unical.campusreport.entity.Zona;
import it.unical.campusreport.entity.enums.Categoria;
import it.unical.campusreport.entity.enums.Priorita;
import it.unical.campusreport.entity.enums.Ruolo;
import it.unical.campusreport.entity.enums.Stato;
import it.unical.campusreport.exception.TicketNotFoundException;
import it.unical.campusreport.exception.ZonaNotFoundException;
import it.unical.campusreport.repository.AllegatoRepository;
import it.unical.campusreport.repository.CambioStatoRepository;
import it.unical.campusreport.repository.MessaggioRepository;
import it.unical.campusreport.repository.TecnicoRepository;
import it.unical.campusreport.repository.TicketRepository;
import it.unical.campusreport.repository.ZonaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTicketServiceImplTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private TecnicoRepository tecnicoRepository;
    @Mock private CambioStatoRepository cambioStatoRepository;
    @Mock private ZonaRepository zonaRepository;
    @Mock private MessaggioRepository messaggioRepository;
    @Mock private AllegatoRepository allegatoRepository;
    @Mock private EmailService emailService;

    private AdminTicketServiceImpl service;

    private User admin;
    private User studente;
    private User tecnico;
    private Zona zona;

    @BeforeEach
    void setUp() {
        service = new AdminTicketServiceImpl(ticketRepository, tecnicoRepository, cambioStatoRepository,
                zonaRepository, messaggioRepository, allegatoRepository, emailService);
        admin = buildUser("Admin", Ruolo.ADMIN);
        studente = buildUser("Mario", Ruolo.STUDENTE);
        tecnico = buildUser("Luigi", Ruolo.TECNICO);
        zona = Zona.builder().id(UUID.randomUUID()).nome("Polo Ingegneria").build();
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

    private Ticket buildTicket(Stato stato, User tecnicoAssegnato) {
        return Ticket.builder()
                .id(UUID.randomUUID())
                .titolo("WiFi assente")
                .descrizione("Il WiFi non funziona")
                .categoria(Categoria.WIFI)
                .stato(stato)
                .priorita(Priorita.NORMALE)
                .urgenza(2)
                .cubo("42C")
                .piano("1")
                .zona(Zona.builder().id(UUID.randomUUID()).nome("Biblioteca").build())
                .segnalante(studente)
                .tecnico(tecnicoAssegnato)
                .build();
    }

    private AdminModificaTicketRequest buildRequest(UUID zonaId, Integer urgenza) {
        AdminModificaTicketRequest request = new AdminModificaTicketRequest();
        request.setTitolo("Presa rotta");
        request.setDescrizione("La presa elettrica è danneggiata");
        request.setCategoria(Categoria.ELETTRICO);
        request.setZonaId(zonaId);
        request.setCubo("41B");
        request.setPiano("2");
        request.setUrgenza(urgenza);
        return request;
    }

    @Test
    void modificaTicket_inLavorazione_aggiornaSenzaRiassegnare() {
        Ticket ticket = buildTicket(Stato.IN_LAVORAZIONE, tecnico);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(zonaRepository.findById(zona.getId())).thenReturn(Optional.of(zona));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cambioStatoRepository.findByTicketOrderByTimestampAsc(ticket)).thenReturn(List.of());

        TicketResponse response = service.modificaTicket(ticket.getId(), buildRequest(zona.getId(), null), admin);

        assertThat(response.getTitolo()).isEqualTo("Presa rotta");
        assertThat(response.getCategoria()).isEqualTo(Categoria.ELETTRICO);
        assertThat(response.getZona().getId()).isEqualTo(zona.getId());
        assertThat(response.getStato()).isEqualTo(Stato.IN_LAVORAZIONE);
        assertThat(response.getUrgenza()).isEqualTo(2);
        assertThat(ticket.getTecnico()).isSameAs(tecnico);
        verifyNoInteractions(tecnicoRepository);

        ArgumentCaptor<CambioStato> captor = ArgumentCaptor.forClass(CambioStato.class);
        verify(cambioStatoRepository).save(captor.capture());
        assertThat(captor.getValue().getStatoPrecedente()).isEqualTo(Stato.IN_LAVORAZIONE);
        assertThat(captor.getValue().getStatoNuovo()).isEqualTo(Stato.IN_LAVORAZIONE);
        assertThat(captor.getValue().getUtente()).isSameAs(admin);
        assertThat(captor.getValue().getNota()).isEqualTo("Segnalazione modificata dall'amministratore");

        verify(emailService).notificaTecnicoModificaSegnalazione(
                ticket, "WiFi assente", "Il WiFi non funziona", Categoria.WIFI, "42C", "1", "dall'amministratore");
    }

    @Test
    void modificaTicket_urgenza5_prioritaAlta() {
        Ticket ticket = buildTicket(Stato.APERTA, null);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(zonaRepository.findById(zona.getId())).thenReturn(Optional.of(zona));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cambioStatoRepository.findByTicketOrderByTimestampAsc(ticket)).thenReturn(List.of());

        TicketResponse response = service.modificaTicket(ticket.getId(), buildRequest(zona.getId(), 5), admin);

        assertThat(response.getUrgenza()).isEqualTo(5);
        assertThat(response.getPriorita()).isEqualTo(Priorita.ALTA);
        verify(emailService, never()).notificaTecnicoModificaSegnalazione(
                any(), anyString(), anyString(), any(), any(), any(), anyString());
    }

    @Test
    void modificaTicket_zonaInesistente_lanciaZonaNotFound() {
        Ticket ticket = buildTicket(Stato.ASSEGNATA, tecnico);
        UUID zonaInesistente = UUID.randomUUID();
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(zonaRepository.findById(zonaInesistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modificaTicket(ticket.getId(), buildRequest(zonaInesistente, null), admin))
                .isInstanceOf(ZonaNotFoundException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void eliminaTicket_eliminaMessaggiStoricoETicketEInviaEmail() {
        Ticket ticket = buildTicket(Stato.ASSEGNATA, tecnico);
        List<CambioStato> storico = List.of(CambioStato.builder().id(UUID.randomUUID()).ticket(ticket).build());
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(cambioStatoRepository.findByTicketOrderByTimestampAsc(ticket)).thenReturn(storico);
        when(allegatoRepository.findByTicket(ticket)).thenReturn(List.of());

        service.eliminaTicket(ticket.getId());

        InOrder ordine = inOrder(messaggioRepository, cambioStatoRepository, allegatoRepository, ticketRepository);
        ordine.verify(messaggioRepository).deleteByTicket(ticket);
        ordine.verify(cambioStatoRepository).deleteAll(storico);
        ordine.verify(allegatoRepository).deleteAll(List.of());
        ordine.verify(ticketRepository).delete(ticket);

        verify(emailService).notificaTicketEliminatoDaAdmin(studente, "WiFi assente");
        verify(emailService).notificaTicketEliminatoDaAdmin(tecnico, "WiFi assente");
    }

    @Test
    void eliminaTicket_inesistente_lanciaTicketNotFound() {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminaTicket(id))
                .isInstanceOf(TicketNotFoundException.class);

        verify(ticketRepository, never()).delete(any(Ticket.class));
        verifyNoInteractions(emailService);
    }
}

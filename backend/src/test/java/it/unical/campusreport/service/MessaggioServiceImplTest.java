package it.unical.campusreport.service;

import it.unical.campusreport.dto.ChatResponse;
import it.unical.campusreport.dto.MessaggioRequest;
import it.unical.campusreport.dto.MessaggioResponse;
import it.unical.campusreport.entity.Messaggio;
import it.unical.campusreport.entity.Ticket;
import it.unical.campusreport.entity.User;
import it.unical.campusreport.entity.Zona;
import it.unical.campusreport.entity.enums.Categoria;
import it.unical.campusreport.entity.enums.Priorita;
import it.unical.campusreport.entity.enums.Ruolo;
import it.unical.campusreport.entity.enums.Stato;
import it.unical.campusreport.exception.ChatNonDisponibileException;
import it.unical.campusreport.exception.UnauthorizedTicketAccessException;
import it.unical.campusreport.repository.MessaggioRepository;
import it.unical.campusreport.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessaggioServiceImplTest {

    @Mock private MessaggioRepository messaggioRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private EmailService emailService;

    private MessaggioServiceImpl service;

    private User studente;
    private User tecnico;

    @BeforeEach
    void setUp() {
        service = new MessaggioServiceImpl(messaggioRepository, ticketRepository, emailService);
        studente = buildUser("Mario", Ruolo.STUDENTE);
        tecnico = buildUser("Luigi", Ruolo.TECNICO);
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

    private Ticket buildTicket(User segnalante, User tecnicoAssegnato) {
        return Ticket.builder()
                .id(UUID.randomUUID())
                .titolo("WiFi assente")
                .descrizione("Il WiFi non funziona")
                .categoria(Categoria.WIFI)
                .stato(tecnicoAssegnato != null ? Stato.ASSEGNATA : Stato.IN_ATTESA)
                .priorita(Priorita.NORMALE)
                .zona(Zona.builder().id(UUID.randomUUID()).nome("Cubo 42").build())
                .segnalante(segnalante)
                .tecnico(tecnicoAssegnato)
                .build();
    }

    private Messaggio buildMessaggio(Ticket ticket, User mittente, String testo, boolean letto) {
        return Messaggio.builder()
                .id(UUID.randomUUID())
                .ticket(ticket)
                .mittente(mittente)
                .testo(testo)
                .createdAt(LocalDateTime.now())
                .letto(letto)
                .build();
    }

    private MessaggioRequest request(String testo) {
        MessaggioRequest request = new MessaggioRequest();
        request.setTesto(testo);
        return request;
    }

    @Test
    void getChat_successo() {
        Ticket ticket = buildTicket(studente, tecnico);
        Messaggio m1 = buildMessaggio(ticket, studente, "Ciao", true);
        Messaggio m2 = buildMessaggio(ticket, tecnico, "Arrivo subito", false);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(messaggioRepository.findByTicketOrderByCreatedAtAsc(ticket)).thenReturn(List.of(m1, m2));

        ChatResponse chat = service.getChat(ticket.getId(), studente);

        assertThat(chat.getTicketId()).isEqualTo(ticket.getId());
        assertThat(chat.getMessaggi()).hasSize(2);
        assertThat(chat.getMessaggi().get(0).isMio()).isTrue();
        assertThat(chat.getMessaggi().get(1).isMio()).isFalse();
        assertThat(chat.getNonLetti()).isEqualTo(1);
    }

    @Test
    void getChat_senzaTecnico() {
        Ticket ticket = buildTicket(studente, null);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.getChat(ticket.getId(), studente))
                .isInstanceOf(ChatNonDisponibileException.class);
        verify(messaggioRepository, never()).findByTicketOrderByCreatedAtAsc(any());
    }

    @Test
    void getChat_nonAutorizzato() {
        Ticket ticket = buildTicket(studente, tecnico);
        User estraneo = buildUser("Anna", Ruolo.STUDENTE);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.getChat(ticket.getId(), estraneo))
                .isInstanceOf(UnauthorizedTicketAccessException.class);
        verify(messaggioRepository, never()).findByTicketOrderByCreatedAtAsc(any());
    }

    @Test
    void inviaMessaggio_daStudente() {
        Ticket ticket = buildTicket(studente, tecnico);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(messaggioRepository.save(any(Messaggio.class))).thenAnswer(inv -> inv.getArgument(0));

        MessaggioResponse response = service.inviaMessaggio(ticket.getId(), request("Quando arrivate?"), studente);

        ArgumentCaptor<Messaggio> captor = ArgumentCaptor.forClass(Messaggio.class);
        verify(messaggioRepository).save(captor.capture());
        assertThat(captor.getValue().getMittente()).isEqualTo(studente);
        assertThat(captor.getValue().getTesto()).isEqualTo("Quando arrivate?");
        assertThat(captor.getValue().isLetto()).isFalse();
        verify(emailService).notificaNuovoMessaggio(captor.getValue(), tecnico);
        assertThat(response.isMio()).isTrue();
        assertThat(response.getTicketId()).isEqualTo(ticket.getId());
    }

    @Test
    void inviaMessaggio_daTecnico() {
        Ticket ticket = buildTicket(studente, tecnico);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(messaggioRepository.save(any(Messaggio.class))).thenAnswer(inv -> inv.getArgument(0));

        service.inviaMessaggio(ticket.getId(), request("Passo domani mattina"), tecnico);

        ArgumentCaptor<Messaggio> captor = ArgumentCaptor.forClass(Messaggio.class);
        verify(messaggioRepository).save(captor.capture());
        assertThat(captor.getValue().getMittente()).isEqualTo(tecnico);
        verify(emailService).notificaNuovoMessaggio(captor.getValue(), studente);
    }

    @Test
    @SuppressWarnings("unchecked")
    void inviaMessaggio_marcaLettiCorrettamente() {
        Ticket ticket = buildTicket(studente, tecnico);
        Messaggio mioNonLetto = buildMessaggio(ticket, tecnico, "Mio messaggio", false);
        Messaggio ricevuto1 = buildMessaggio(ticket, studente, "Primo", false);
        Messaggio ricevuto2 = buildMessaggio(ticket, studente, "Secondo", false);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(messaggioRepository.findByTicketOrderByCreatedAtAsc(ticket))
                .thenReturn(List.of(ricevuto1, mioNonLetto, ricevuto2));

        ChatResponse chat = service.getChat(ticket.getId(), tecnico);

        assertThat(chat.getNonLetti()).isEqualTo(2);
        assertThat(ricevuto1.isLetto()).isTrue();
        assertThat(ricevuto2.isLetto()).isTrue();
        // I messaggi scritti dall'utente corrente non vanno marcati come letti
        assertThat(mioNonLetto.isLetto()).isFalse();

        ArgumentCaptor<List<Messaggio>> captor = ArgumentCaptor.forClass(List.class);
        verify(messaggioRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(ricevuto1, ricevuto2);
    }

    @Test
    void getChat_nessunNonLetto_nonSalva() {
        Ticket ticket = buildTicket(studente, tecnico);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(messaggioRepository.findByTicketOrderByCreatedAtAsc(ticket))
                .thenReturn(List.of(buildMessaggio(ticket, tecnico, "Già letto", true)));

        ChatResponse chat = service.getChat(ticket.getId(), studente);

        assertThat(chat.getNonLetti()).isZero();
        verify(messaggioRepository, never()).saveAll(anyList());
    }
}

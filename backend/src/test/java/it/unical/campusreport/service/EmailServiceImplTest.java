package it.unical.campusreport.service;

import it.unical.campusreport.entity.Ticket;
import it.unical.campusreport.entity.User;
import it.unical.campusreport.entity.Zona;
import it.unical.campusreport.entity.enums.Categoria;
import it.unical.campusreport.entity.enums.Priorita;
import it.unical.campusreport.entity.enums.Ruolo;
import it.unical.campusreport.entity.enums.Stato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    private static final String FROM = "noreply@campusreport.local";
    private static final String ADMIN_EMAIL = "admin@campusreport.local";
    private static final String FRONTEND_URL = "http://localhost:4200";

    @Mock private JavaMailSender mailSender;

    private EmailServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmailServiceImpl(mailSender, FROM, ADMIN_EMAIL, FRONTEND_URL);
    }

    private Zona buildZona() {
        return Zona.builder().id(UUID.randomUUID()).nome("Polo Ingegneria Cubi 37-46").build();
    }

    private User buildUser(String nome, String email, Ruolo ruolo) {
        return User.builder()
                .id(UUID.randomUUID())
                .nome(nome)
                .cognome("Rossi")
                .email(email)
                .passwordHash("hash")
                .ruolo(ruolo)
                .attivo(true)
                .build();
    }

    private Ticket buildTicket(User segnalante) {
        return Ticket.builder()
                .id(UUID.randomUUID())
                .titolo("WiFi assente in aula")
                .descrizione("Il WiFi non funziona in aula magna")
                .categoria(Categoria.WIFI)
                .stato(Stato.APERTA)
                .priorita(Priorita.NORMALE)
                .zona(buildZona())
                .segnalante(segnalante)
                .build();
    }

    // ─── notificaTecnicoNuovaAssegnazione ────────────────────────────────────────

    @Test
    void notificaTecnicoNuovaAssegnazione_inviaEmailConOggettoECorpoAttesi() {
        User segnalante = buildUser("Luca", "luca@studenti.unical.it", Ruolo.STUDENTE);
        User tecnico = buildUser("Mario", "mario@campusreport.local", Ruolo.TECNICO);
        Ticket ticket = buildTicket(segnalante);

        service.notificaTecnicoNuovaAssegnazione(ticket, tecnico);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();

        assertThat(message.getFrom()).isEqualTo(FROM);
        assertThat(message.getTo()).containsExactly("mario@campusreport.local");
        assertThat(message.getSubject())
                .isEqualTo("[CampusReport] Nuova segnalazione assegnata - WiFi assente in aula");
        assertThat(message.getText())
                .contains("Gentile Mario,")
                .contains("- Titolo: WiFi assente in aula")
                .contains("- Categoria: WIFI")
                .contains("- Zona: Polo Ingegneria Cubi 37-46")
                .contains("- Priorità: NORMALE")
                .contains("- Descrizione: Il WiFi non funziona in aula magna");
    }

    // ─── notificaUtenteStatoCambiato ─────────────────────────────────────────────

    @Test
    void notificaUtenteStatoCambiato_completata_usaOggettoEMessaggioCorretti() {
        User segnalante = buildUser("Luca", "luca@studenti.unical.it", Ruolo.STUDENTE);
        Ticket ticket = buildTicket(segnalante);

        service.notificaUtenteStatoCambiato(ticket, Stato.COMPLETATA);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();

        assertThat(message.getTo()).containsExactly("luca@studenti.unical.it");
        assertThat(message.getSubject()).isEqualTo("[CampusReport] Segnalazione risolta");
        assertThat(message.getText())
                .contains("Gentile Luca,")
                .contains("Stato attuale: COMPLETATA")
                .contains("Il problema è stato risolto. Grazie per la segnalazione.");
    }

    @Test
    void notificaUtenteStatoCambiato_inAttesa_usaOggettoEMessaggioCorretti() {
        User segnalante = buildUser("Anna", "anna@unical.it", Ruolo.DOCENTE);
        Ticket ticket = buildTicket(segnalante);

        service.notificaUtenteStatoCambiato(ticket, Stato.IN_ATTESA);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();

        assertThat(message.getSubject()).isEqualTo("[CampusReport] Segnalazione in attesa");
        assertThat(message.getText())
                .contains("Nessun tecnico disponibile al momento. Sarà contattato appena possibile.");
    }

    // ─── notificaAdminTicketInAttesa ─────────────────────────────────────────────

    @Test
    void notificaAdminTicketInAttesa_inviaEmailAllAdminConDettagliTicket() {
        User segnalante = buildUser("Luca", "luca@studenti.unical.it", Ruolo.STUDENTE);
        Ticket ticket = buildTicket(segnalante);

        service.notificaAdminTicketInAttesa(ticket);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();

        assertThat(message.getTo()).containsExactly(ADMIN_EMAIL);
        assertThat(message.getSubject())
                .isEqualTo("[CampusReport] ATTENZIONE: Ticket in attesa di assegnazione");
        assertThat(message.getText())
                .contains("- ID: " + ticket.getId())
                .contains("- Segnalante: luca@studenti.unical.it");
    }

    // ─── Gestione errori: nessuna eccezione propagata ────────────────────────────

    @Test
    void notificaTecnicoNuovaAssegnazione_erroreInvio_nonPropagaEccezione() {
        User segnalante = buildUser("Luca", "luca@studenti.unical.it", Ruolo.STUDENTE);
        User tecnico = buildUser("Mario", "mario@campusreport.local", Ruolo.TECNICO);
        Ticket ticket = buildTicket(segnalante);

        doThrow(new MailSendException("SMTP non raggiungibile")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> service.notificaTecnicoNuovaAssegnazione(ticket, tecnico))
                .doesNotThrowAnyException();
    }

    @Test
    void notificaUtenteStatoCambiato_erroreInvio_nonPropagaEccezione() {
        User segnalante = buildUser("Luca", "luca@studenti.unical.it", Ruolo.STUDENTE);
        Ticket ticket = buildTicket(segnalante);

        doThrow(new MailSendException("SMTP non raggiungibile")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> service.notificaUtenteStatoCambiato(ticket, Stato.IN_LAVORAZIONE))
                .doesNotThrowAnyException();
    }

    @Test
    void notificaAdminTicketInAttesa_erroreInvio_nonPropagaEccezione() {
        User segnalante = buildUser("Luca", "luca@studenti.unical.it", Ruolo.STUDENTE);
        Ticket ticket = buildTicket(segnalante);

        doThrow(new MailSendException("SMTP non raggiungibile")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> service.notificaAdminTicketInAttesa(ticket))
                .doesNotThrowAnyException();
    }
}

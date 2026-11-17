package it.unical.campusreport.service;

import it.unical.campusreport.entity.CambioStato;
import it.unical.campusreport.entity.Tecnico;
import it.unical.campusreport.entity.Ticket;
import it.unical.campusreport.entity.enums.Categoria;
import it.unical.campusreport.entity.enums.Ruolo;
import it.unical.campusreport.entity.enums.Stato;
import it.unical.campusreport.exception.TecnicoNonEliminabileException;
import it.unical.campusreport.exception.TecnicoNotFoundException;
import it.unical.campusreport.repository.CambioStatoRepository;
import it.unical.campusreport.repository.MessaggioRepository;
import it.unical.campusreport.repository.PasswordResetTokenRepository;
import it.unical.campusreport.repository.TecnicoRepository;
import it.unical.campusreport.repository.TicketRepository;
import it.unical.campusreport.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTecnicoServiceImplTest {

    @Mock private TecnicoRepository tecnicoRepository;
    @Mock private UserRepository userRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private CambioStatoRepository cambioStatoRepository;
    @Mock private MessaggioRepository messaggioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    private AdminTecnicoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminTecnicoServiceImpl(tecnicoRepository, userRepository, ticketRepository,
                passwordResetTokenRepository, cambioStatoRepository, messaggioRepository,
                passwordEncoder, emailService);
    }

    private Tecnico buildTecnico(boolean attivo) {
        return Tecnico.builder()
                .id(UUID.randomUUID())
                .nome("Luigi")
                .cognome("Verdi")
                .email("luigi@test.it")
                .passwordHash("hash")
                .ruolo(Ruolo.TECNICO)
                .attivo(attivo)
                .specializzazioni(List.of(Categoria.WIFI))
                .zona("Polo Ingegneria")
                .caricoMassimo(10)
                .build();
    }

    @Test
    void eliminaTecnico_disattivatoSenzaTicket_elimina() {
        Tecnico tecnico = buildTecnico(false);
        when(tecnicoRepository.findById(tecnico.getId())).thenReturn(Optional.of(tecnico));
        when(ticketRepository.countByTecnicoAndStatoIn(eq(tecnico), anyList())).thenReturn(0L);
        when(ticketRepository.findByTecnico(tecnico)).thenReturn(List.of());
        when(cambioStatoRepository.findByUtente(tecnico)).thenReturn(List.of());

        service.eliminaTecnico(tecnico.getId());

        verify(messaggioRepository).deleteByMittente(tecnico);
        verify(passwordResetTokenRepository).deleteByUser(tecnico);
        verify(tecnicoRepository).delete(tecnico);
    }

    @Test
    void eliminaTecnico_attivo_lanciaEccezione() {
        Tecnico tecnico = buildTecnico(true);
        when(tecnicoRepository.findById(tecnico.getId())).thenReturn(Optional.of(tecnico));

        assertThatThrownBy(() -> service.eliminaTecnico(tecnico.getId()))
                .isInstanceOf(TecnicoNonEliminabileException.class)
                .hasMessageContaining("disattivato");

        verify(tecnicoRepository, never()).delete(any());
    }

    @Test
    void eliminaTecnico_conTicketInCorso_lanciaEccezione() {
        Tecnico tecnico = buildTecnico(false);
        when(tecnicoRepository.findById(tecnico.getId())).thenReturn(Optional.of(tecnico));
        when(ticketRepository.countByTecnicoAndStatoIn(eq(tecnico), anyList())).thenReturn(2L);

        assertThatThrownBy(() -> service.eliminaTecnico(tecnico.getId()))
                .isInstanceOf(TecnicoNonEliminabileException.class)
                .hasMessageContaining("2 segnalazioni in corso");

        verify(tecnicoRepository, never()).delete(any());
    }

    @Test
    void eliminaTecnico_conTicketStorici_restanoSenzaTecnico() {
        Tecnico tecnico = buildTecnico(false);
        Ticket completato = Ticket.builder().id(UUID.randomUUID()).stato(Stato.COMPLETATA).tecnico(tecnico).build();
        Ticket rifiutato = Ticket.builder().id(UUID.randomUUID()).stato(Stato.RIFIUTATA).tecnico(tecnico).build();
        CambioStato cambio = CambioStato.builder().id(UUID.randomUUID()).ticket(completato).utente(tecnico).build();

        when(tecnicoRepository.findById(tecnico.getId())).thenReturn(Optional.of(tecnico));
        when(ticketRepository.countByTecnicoAndStatoIn(eq(tecnico), anyList())).thenReturn(0L);
        when(ticketRepository.findByTecnico(tecnico)).thenReturn(List.of(completato, rifiutato));
        when(cambioStatoRepository.findByUtente(tecnico)).thenReturn(List.of(cambio));

        service.eliminaTecnico(tecnico.getId());

        assertThat(completato.getTecnico()).isNull();
        assertThat(rifiutato.getTecnico()).isNull();
        assertThat(cambio.getUtente()).isNull();
        verify(ticketRepository).saveAll(List.of(completato, rifiutato));
        verify(cambioStatoRepository).saveAll(List.of(cambio));
        verify(ticketRepository, never()).delete(any(Ticket.class));
        verify(tecnicoRepository).delete(tecnico);
    }

    @Test
    void eliminaTecnico_inesistente_lanciaTecnicoNotFound() {
        UUID id = UUID.randomUUID();
        when(tecnicoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminaTecnico(id))
                .isInstanceOf(TecnicoNotFoundException.class);
    }
}

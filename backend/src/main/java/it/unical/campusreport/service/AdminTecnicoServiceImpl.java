package it.unical.campusreport.service;

import it.unical.campusreport.dto.CambioStatoTecnicoRequest;
import it.unical.campusreport.dto.CreaTecnicoRequest;
import it.unical.campusreport.dto.ModificaTecnicoRequest;
import it.unical.campusreport.dto.TecnicoAdminResponse;
import it.unical.campusreport.entity.CambioStato;
import it.unical.campusreport.entity.PasswordResetToken;
import it.unical.campusreport.entity.Tecnico;
import it.unical.campusreport.entity.Ticket;
import it.unical.campusreport.entity.enums.Ruolo;
import it.unical.campusreport.entity.enums.Stato;
import it.unical.campusreport.exception.EmailAlreadyExistsException;
import it.unical.campusreport.exception.TecnicoNonEliminabileException;
import it.unical.campusreport.exception.TecnicoNotFoundException;
import it.unical.campusreport.repository.CambioStatoRepository;
import it.unical.campusreport.repository.MessaggioRepository;
import it.unical.campusreport.repository.PasswordResetTokenRepository;
import it.unical.campusreport.repository.TecnicoRepository;
import it.unical.campusreport.repository.TicketRepository;
import it.unical.campusreport.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementazione di {@link AdminTecnicoService}.
 */
@Service
@Slf4j
public class AdminTecnicoServiceImpl implements AdminTecnicoService {

    private static final int SCADENZA_TOKEN_ORE = 48;

    private final TecnicoRepository tecnicoRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final CambioStatoRepository cambioStatoRepository;
    private final MessaggioRepository messaggioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AdminTecnicoServiceImpl(TecnicoRepository tecnicoRepository,
                                   UserRepository userRepository,
                                   TicketRepository ticketRepository,
                                   PasswordResetTokenRepository passwordResetTokenRepository,
                                   CambioStatoRepository cambioStatoRepository,
                                   MessaggioRepository messaggioRepository,
                                   PasswordEncoder passwordEncoder,
                                   EmailService emailService) {
        this.tecnicoRepository = tecnicoRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.cambioStatoRepository = cambioStatoRepository;
        this.messaggioRepository = messaggioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<TecnicoAdminResponse> getAllTecnici() {
        log.debug("Recupero elenco tecnici per il pannello admin");
        return tecnicoRepository.findAll().stream()
                .map(t -> toTecnicoAdminResponse(t, caricoAttualeDi(t)))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public TecnicoAdminResponse creaTecnico(CreaTecnicoRequest request) {
        log.info("Creazione nuovo tecnico: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email già esistente: " + request.getEmail());
        }

        // Password temporanea inutilizzabile: il tecnico la imposta tramite il link di attivazione.
        String passwordTemporanea = UUID.randomUUID().toString();

        Tecnico tecnico = Tecnico.builder()
                .nome(request.getNome())
                .cognome(request.getCognome())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(passwordTemporanea))
                .ruolo(Ruolo.TECNICO)
                .attivo(false)
                .specializzazioni(request.getSpecializzazioni())
                .zona(request.getZona())
                .caricoMassimo(request.getCaricoMassimo())
                .build();
        tecnico = tecnicoRepository.save(tecnico);

        String token = UUID.randomUUID().toString();
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(tecnico)
                .token(token)
                .scadenza(LocalDateTime.now().plusHours(SCADENZA_TOKEN_ORE))
                .usato(false)
                .build());

        emailService.inviaInvitoTecnico(tecnico, token);

        log.info("Tecnico {} creato con successo, in attesa di attivazione", tecnico.getEmail());
        return toTecnicoAdminResponse(tecnico, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public TecnicoAdminResponse modificaTecnico(UUID id, ModificaTecnicoRequest request) {
        log.info("Modifica tecnico {}", id);

        Tecnico tecnico = tecnicoRepository.findById(id)
                .orElseThrow(() -> new TecnicoNotFoundException("Tecnico non trovato con id: " + id));

        tecnico.setSpecializzazioni(request.getSpecializzazioni());
        tecnico.setZona(request.getZona());
        tecnico.setCaricoMassimo(request.getCaricoMassimo());
        tecnico = tecnicoRepository.save(tecnico);

        log.info("Tecnico {} modificato", tecnico.getEmail());
        return toTecnicoAdminResponse(tecnico, caricoAttualeDi(tecnico));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public TecnicoAdminResponse cambiaStato(UUID id, CambioStatoTecnicoRequest request) {
        log.info("Cambio stato attivazione tecnico {} → {}", id, request.isAttivo());

        Tecnico tecnico = tecnicoRepository.findById(id)
                .orElseThrow(() -> new TecnicoNotFoundException("Tecnico non trovato con id: " + id));

        tecnico.setAttivo(request.isAttivo());
        tecnico = tecnicoRepository.save(tecnico);

        return toTecnicoAdminResponse(tecnico, caricoAttualeDi(tecnico));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Prima di eliminare il tecnico lo stacca dallo storico per rispettare le FK:
     * i ticket e i cambi di stato che lo referenziano restano con tecnico/utente null,
     * mentre messaggi di chat e token di attivazione vengono eliminati. La strategia
     * JOINED rimuove anche la riga in {@code users} e le specializzazioni in cascata.
     */
    @Override
    @Transactional
    public void eliminaTecnico(UUID id) {
        log.info("Richiesta eliminazione tecnico {}", id);

        Tecnico tecnico = tecnicoRepository.findById(id)
                .orElseThrow(() -> new TecnicoNotFoundException("Tecnico non trovato con id: " + id));

        if (tecnico.isAttivo()) {
            throw new TecnicoNonEliminabileException(
                    "Il tecnico deve essere disattivato prima di poter essere eliminato");
        }

        int ticketInCorso = caricoAttualeDi(tecnico);
        if (ticketInCorso > 0) {
            throw new TecnicoNonEliminabileException(
                    "Il tecnico ha " + ticketInCorso + " segnalazioni in corso. Riassegnale prima di eliminarlo");
        }

        List<Ticket> ticketStorici = ticketRepository.findByTecnico(tecnico);
        for (Ticket ticket : ticketStorici) {
            ticket.setTecnico(null);
        }
        ticketRepository.saveAll(ticketStorici);

        List<CambioStato> cambiStato = cambioStatoRepository.findByUtente(tecnico);
        for (CambioStato cambioStato : cambiStato) {
            cambioStato.setUtente(null);
        }
        cambioStatoRepository.saveAll(cambiStato);

        messaggioRepository.deleteByMittente(tecnico);
        passwordResetTokenRepository.deleteByUser(tecnico);

        tecnicoRepository.delete(tecnico);

        log.info("Tecnico {} eliminato definitivamente ({} ticket storici scollegati)",
                tecnico.getEmail(), ticketStorici.size());
    }

    // ─── Helper privati ─────────────────────────────────────────────────────────

    private int caricoAttualeDi(Tecnico tecnico) {
        return (int) ticketRepository.countByTecnicoAndStatoIn(tecnico, List.of(Stato.ASSEGNATA, Stato.IN_LAVORAZIONE));
    }

    private TecnicoAdminResponse toTecnicoAdminResponse(Tecnico tecnico, int caricoAttuale) {
        return TecnicoAdminResponse.builder()
                .id(tecnico.getId())
                .nome(tecnico.getNome())
                .cognome(tecnico.getCognome())
                .email(tecnico.getEmail())
                .specializzazioni(tecnico.getSpecializzazioni())
                .zona(tecnico.getZona())
                .caricoMassimo(tecnico.getCaricoMassimo())
                .caricoAttuale(caricoAttuale)
                .attivo(tecnico.isAttivo())
                .build();
    }
}

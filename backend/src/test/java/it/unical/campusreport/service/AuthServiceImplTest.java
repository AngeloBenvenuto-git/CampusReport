package it.unical.campusreport.service;

import it.unical.campusreport.dto.AttivaAccountRequest;
import it.unical.campusreport.dto.AuthResponse;
import it.unical.campusreport.dto.VerificaTokenResponse;
import it.unical.campusreport.entity.PasswordResetToken;
import it.unical.campusreport.entity.User;
import it.unical.campusreport.entity.enums.Ruolo;
import it.unical.campusreport.exception.PasswordNonCoincidentiException;
import it.unical.campusreport.exception.TokenNonValidoException;
import it.unical.campusreport.exception.TokenScadutoException;
import it.unical.campusreport.repository.PasswordResetTokenRepository;
import it.unical.campusreport.repository.UserRepository;
import it.unical.campusreport.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String TOKEN = "token-attivazione";

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(userRepository, passwordResetTokenRepository, jwtService, passwordEncoder, false);
    }

    private User buildTecnicoNonAttivo() {
        return User.builder()
                .id(UUID.randomUUID())
                .nome("Mario")
                .cognome("Rossi")
                .email("mario.rossi@campusreport.local")
                .passwordHash("hash-temporaneo")
                .ruolo(Ruolo.TECNICO)
                .attivo(false)
                .build();
    }

    private PasswordResetToken buildToken(User user, LocalDateTime scadenza) {
        return PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token(TOKEN)
                .scadenza(scadenza)
                .usato(false)
                .build();
    }

    private AttivaAccountRequest buildRequest(String password, String conferma) {
        AttivaAccountRequest request = new AttivaAccountRequest();
        request.setToken(TOKEN);
        request.setPassword(password);
        request.setConfermaPassword(conferma);
        return request;
    }

    // ─── attivaAccount ──────────────────────────────────────────────────────────

    @Test
    void attivaAccount_successo() {
        User tecnico = buildTecnicoNonAttivo();
        PasswordResetToken token = buildToken(tecnico, LocalDateTime.now().plusHours(24));
        when(passwordResetTokenRepository.findByTokenAndUsatoFalse(TOKEN)).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("Password123")).thenReturn("nuovo-hash");
        when(jwtService.generateToken(tecnico)).thenReturn("jwt");

        AuthResponse response = service.attivaAccount(buildRequest("Password123", "Password123"));

        assertThat(tecnico.isAttivo()).isTrue();
        assertThat(tecnico.getPasswordHash()).isEqualTo("nuovo-hash");
        assertThat(token.isUsato()).isTrue();
        verify(userRepository).save(tecnico);
        verify(passwordResetTokenRepository).save(token);
        assertThat(response.getToken()).isEqualTo("jwt");
        assertThat(response.getRuolo()).isEqualTo("TECNICO");
        assertThat(response.getEmail()).isEqualTo(tecnico.getEmail());
    }

    @Test
    void attivaAccount_tokenNonValido() {
        when(passwordResetTokenRepository.findByTokenAndUsatoFalse(TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.attivaAccount(buildRequest("Password123", "Password123")))
                .isInstanceOf(TokenNonValidoException.class)
                .hasMessage(AuthServiceImpl.MSG_TOKEN_NON_VALIDO);
        verify(userRepository, never()).save(any());
    }

    @Test
    void attivaAccount_tokenScaduto() {
        User tecnico = buildTecnicoNonAttivo();
        PasswordResetToken token = buildToken(tecnico, LocalDateTime.now().minusMinutes(1));
        when(passwordResetTokenRepository.findByTokenAndUsatoFalse(TOKEN)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.attivaAccount(buildRequest("Password123", "Password123")))
                .isInstanceOf(TokenScadutoException.class)
                .hasMessage(AuthServiceImpl.MSG_TOKEN_SCADUTO);
        assertThat(tecnico.isAttivo()).isFalse();
        assertThat(token.isUsato()).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void attivaAccount_passwordNonCoincidono() {
        User tecnico = buildTecnicoNonAttivo();
        PasswordResetToken token = buildToken(tecnico, LocalDateTime.now().plusHours(24));
        when(passwordResetTokenRepository.findByTokenAndUsatoFalse(TOKEN)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.attivaAccount(buildRequest("Password123", "Password456")))
                .isInstanceOf(PasswordNonCoincidentiException.class)
                .hasMessage("Le password non coincidono");
        assertThat(tecnico.isAttivo()).isFalse();
        assertThat(token.isUsato()).isFalse();
        verify(userRepository, never()).save(any());
    }

    // ─── verificaToken ──────────────────────────────────────────────────────────

    @Test
    void verificaToken_valido_restituisceDatiUtente() {
        User tecnico = buildTecnicoNonAttivo();
        when(passwordResetTokenRepository.findByTokenAndUsatoFalse(TOKEN))
                .thenReturn(Optional.of(buildToken(tecnico, LocalDateTime.now().plusHours(24))));

        VerificaTokenResponse response = service.verificaToken(TOKEN);

        assertThat(response.isValido()).isTrue();
        assertThat(response.getNome()).isEqualTo("Mario");
        assertThat(response.getCognome()).isEqualTo("Rossi");
        assertThat(response.getEmail()).isEqualTo(tecnico.getEmail());
        assertThat(response.getMotivo()).isNull();
    }

    @Test
    void verificaToken_scaduto_restituisceMotivo() {
        when(passwordResetTokenRepository.findByTokenAndUsatoFalse(TOKEN))
                .thenReturn(Optional.of(buildToken(buildTecnicoNonAttivo(), LocalDateTime.now().minusHours(1))));

        VerificaTokenResponse response = service.verificaToken(TOKEN);

        assertThat(response.isValido()).isFalse();
        assertThat(response.getMotivo()).isEqualTo(AuthServiceImpl.MSG_TOKEN_SCADUTO);
        assertThat(response.getEmail()).isNull();
    }

    @Test
    void verificaToken_inesistente_restituisceMotivo() {
        when(passwordResetTokenRepository.findByTokenAndUsatoFalse(TOKEN)).thenReturn(Optional.empty());

        VerificaTokenResponse response = service.verificaToken(TOKEN);

        assertThat(response.isValido()).isFalse();
        assertThat(response.getMotivo()).isEqualTo(AuthServiceImpl.MSG_TOKEN_NON_VALIDO);
    }
}

package it.unical.campusreport.service;

import it.unical.campusreport.dto.AuthResponse;
import it.unical.campusreport.dto.LoginRequest;
import it.unical.campusreport.dto.RegisterRequest;
import it.unical.campusreport.entity.User;
import it.unical.campusreport.entity.enums.Ruolo;
import it.unical.campusreport.exception.EmailAlreadyExistsException;
import it.unical.campusreport.exception.InvalidCredentialsException;
import it.unical.campusreport.exception.InvalidDomainException;
import it.unical.campusreport.exception.UserNotActiveException;
import it.unical.campusreport.repository.UserRepository;
import it.unical.campusreport.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementazione di {@link AuthService} con logica di registrazione e login.
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final boolean domainValidation;

    public AuthServiceImpl(UserRepository userRepository,
                           JwtService jwtService,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.email.domain-validation}") boolean domainValidation) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.domainValidation = domainValidation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.debug("Tentativo registrazione per email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email già registrata: " + request.getEmail());
        }

        Ruolo ruolo = determineRuolo(request.getEmail());

        User user = User.builder()
                .nome(request.getNome())
                .cognome(request.getCognome())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .ruolo(ruolo)
                .attivo(true)
                .build();

        User saved = userRepository.save(user);
        log.info("Nuovo utente registrato: {} con ruolo {}", saved.getEmail(), saved.getRuolo());

        String token = jwtService.generateToken(saved);
        return buildAuthResponse(token, saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.debug("Tentativo login per email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Credenziali non valide"));

        if (!user.isAttivo()) {
            throw new UserNotActiveException("Account non attivo");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Credenziali non valide");
        }

        log.info("Login effettuato: {}", user.getEmail());

        String token = jwtService.generateToken(user);
        return buildAuthResponse(token, user);
    }

    /**
     * Determina il ruolo in base al dominio email.
     * In profilo dev (@unical.it → DOCENTE, altrimenti → STUDENTE).
     * In profilo prod: solo @studenti.unical.it o @unical.it.
     */
    private Ruolo determineRuolo(String email) {
        if (!domainValidation) {
            if (email.endsWith("@unical.it") && !email.endsWith("@studenti.unical.it")) {
                return Ruolo.DOCENTE;
            }
            return Ruolo.STUDENTE;
        }
        if (email.endsWith("@studenti.unical.it")) {
            return Ruolo.STUDENTE;
        }
        if (email.endsWith("@unical.it")) {
            return Ruolo.DOCENTE;
        }
        throw new InvalidDomainException("Dominio email non valido. Usa @studenti.unical.it o @unical.it");
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .id(user.getId())
                .email(user.getEmail())
                .ruolo(user.getRuolo().name())
                .nome(user.getNome())
                .cognome(user.getCognome())
                .build();
    }
}

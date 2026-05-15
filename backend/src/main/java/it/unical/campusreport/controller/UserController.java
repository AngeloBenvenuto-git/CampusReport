package it.unical.campusreport.controller;

import it.unical.campusreport.dto.UserResponse;
import it.unical.campusreport.entity.User;
import it.unical.campusreport.exception.UserNotFoundException;
import it.unical.campusreport.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller per le operazioni relative all'utente autenticato.
 */
@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Restituisce il profilo dell'utente attualmente autenticato.
     *
     * @return dati dell'utente loggato
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getMe() {
        User utente = getCurrentUser();
        log.debug("Recupero profilo utente: {}", utente.getEmail());
        return ResponseEntity.ok(toUserResponse(utente));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato: " + auth.getName()));
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .nome(user.getNome())
                .cognome(user.getCognome())
                .email(user.getEmail())
                .ruolo(user.getRuolo())
                .build();
    }
}

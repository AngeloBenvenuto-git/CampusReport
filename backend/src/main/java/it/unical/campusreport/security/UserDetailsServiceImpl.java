package it.unical.campusreport.security;

import it.unical.campusreport.entity.User;
import it.unical.campusreport.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementazione di {@link UserDetailsService} che carica gli utenti dal database
 * tramite {@link UserRepository}.
 */
@Service
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Carica un utente per email. Lancia {@link UsernameNotFoundException} se non trovato
     * e {@link DisabledException} se l'account non è attivo.
     *
     * @param email l'indirizzo email dell'utente (username)
     * @return i dettagli dell'utente per Spring Security
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Caricamento utente per email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + email));

        if (!user.isAttivo()) {
            throw new DisabledException("Account non attivo per: " + email);
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRuolo().name())))
                .build();
    }
}

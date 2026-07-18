package it.unical.campusreport.entity;

import it.unical.campusreport.entity.enums.Stato;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cambio_stato")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CambioStato {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    @ToString.Exclude
    private Ticket ticket;

    // Nullable: la prima transizione di un ticket (null → APERTA) non ha uno stato precedente
    @Enumerated(EnumType.STRING)
    @Column(name = "stato_precedente", nullable = true)
    private Stato statoPrecedente;

    @Enumerated(EnumType.STRING)
    @Column(name = "stato_nuovo", nullable = false)
    private Stato statoNuovo;

    // Nullable: le transizioni di sistema (es. APERTA→IN_ATTESA senza tecnico) non hanno un utente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utente_id", nullable = true)
    @ToString.Exclude
    private User utente;

    @Column(name = "nota", nullable = true)
    private String nota;

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false)
    private LocalDateTime timestamp;
}

package it.unical.campusreport.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Messaggio della chat tra segnalante e tecnico assegnato, legato a un ticket.
 */
@Entity
@Table(name = "messaggi")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Messaggio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    @ToString.Exclude
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mittente_id", nullable = false)
    @ToString.Exclude
    private User mittente;

    @Column(nullable = false, length = 2000)
    private String testo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean letto = false;
}

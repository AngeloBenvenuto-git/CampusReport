package it.unical.campusreport.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "allegati")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Allegato {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    @ToString.Exclude
    private Ticket ticket;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private String mimetype;

    @Column(nullable = false)
    private long dimensione;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

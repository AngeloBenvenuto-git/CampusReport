package it.unical.campusreport.entity;

import it.unical.campusreport.entity.enums.Categoria;
import it.unical.campusreport.entity.enums.Priorita;
import it.unical.campusreport.entity.enums.Stato;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String titolo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descrizione;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Stato stato;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priorita priorita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_id", nullable = false)
    @ToString.Exclude
    private Zona zona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segnalante_id", nullable = false)
    @ToString.Exclude
    private User segnalante;

    // Nullable: non ancora assegnato o in attesa
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id", nullable = true)
    @ToString.Exclude
    private User tecnico;

    // Nullable: valorizzato dopo la chiamata al microservizio NLP, null al momento dell'insert iniziale
    @Column(name = "categoria_confidenza", nullable = true)
    private Float categoriaConfidenza;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

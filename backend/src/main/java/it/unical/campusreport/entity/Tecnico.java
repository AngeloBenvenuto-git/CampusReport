package it.unical.campusreport.entity;

import it.unical.campusreport.entity.enums.Categoria;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Table(name = "tecnici")
@DiscriminatorValue("TECNICO")
@PrimaryKeyJoinColumn(name = "user_id")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Tecnico extends User {

    @ElementCollection
    @CollectionTable(
            name = "tecnico_specializzazioni",
            joinColumns = @JoinColumn(name = "tecnico_id")
    )
    @Column(name = "categoria")
    @Enumerated(EnumType.STRING)
    private List<Categoria> specializzazioni;

    private String zona;

    // Il default 10 è valido per il costruttore no-args (usato da JPA).
    // Quando si usa il builder impostare esplicitamente caricoMassimo(10).
    @Column(name = "carico_massimo", nullable = false)
    private int caricoMassimo = 10;
}

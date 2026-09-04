package it.unical.campusreport.repository;

import it.unical.campusreport.entity.Ticket;
import it.unical.campusreport.entity.User;
import it.unical.campusreport.entity.enums.Categoria;
import it.unical.campusreport.entity.enums.Stato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

    List<Ticket> findBySegnalante(User segnalante);

    List<Ticket> findByTecnico(User tecnico);

    List<Ticket> findByStato(Stato stato);

    List<Ticket> findByTecnicoAndStatoIn(User tecnico, List<Stato> stati);

    long countByTecnicoAndStatoIn(User tecnico, List<Stato> stati);

    /**
     * Usata dal pannello admin per l'elenco completo dei ticket ordinato per
     * priorità operativa: stato, poi priorità, poi data di creazione decrescente.
     */
    List<Ticket> findAllByOrderByStatoAscPrioritaDescCreatedAtDesc();

    long countByStato(Stato stato);

    long countByCategoria(Categoria categoria);

    long countByStatoAndTecnico(Stato stato, User tecnico);
}

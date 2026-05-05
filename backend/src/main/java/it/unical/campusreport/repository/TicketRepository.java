package it.unical.campusreport.repository;

import it.unical.campusreport.entity.Ticket;
import it.unical.campusreport.entity.User;
import it.unical.campusreport.entity.enums.Stato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findBySegnalante(User segnalante);

    List<Ticket> findByTecnico(User tecnico);

    List<Ticket> findByStato(Stato stato);

    List<Ticket> findByTecnicoAndStatoIn(User tecnico, List<Stato> stati);

    long countByTecnicoAndStatoIn(User tecnico, List<Stato> stati);
}

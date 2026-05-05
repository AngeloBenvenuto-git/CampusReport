package it.unical.campusreport.repository;

import it.unical.campusreport.entity.Allegato;
import it.unical.campusreport.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AllegatoRepository extends JpaRepository<Allegato, UUID> {

    List<Allegato> findByTicket(Ticket ticket);

    long countByTicket(Ticket ticket);
}

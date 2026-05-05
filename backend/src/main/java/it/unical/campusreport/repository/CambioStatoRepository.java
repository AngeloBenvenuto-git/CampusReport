package it.unical.campusreport.repository;

import it.unical.campusreport.entity.CambioStato;
import it.unical.campusreport.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CambioStatoRepository extends JpaRepository<CambioStato, UUID> {

    List<CambioStato> findByTicketOrderByTimestampAsc(Ticket ticket);
}

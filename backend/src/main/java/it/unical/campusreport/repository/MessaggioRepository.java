package it.unical.campusreport.repository;

import it.unical.campusreport.entity.Messaggio;
import it.unical.campusreport.entity.Ticket;
import it.unical.campusreport.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessaggioRepository extends JpaRepository<Messaggio, UUID> {

    List<Messaggio> findByTicketOrderByCreatedAtAsc(Ticket ticket);

    long countByTicketAndLettoFalseAndMittenteNot(Ticket ticket, User mittente);

    List<Messaggio> findByTicketAndLettoFalseAndMittenteNot(Ticket ticket, User mittente);
}

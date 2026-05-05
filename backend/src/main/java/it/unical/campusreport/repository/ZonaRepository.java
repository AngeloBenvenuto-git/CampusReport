package it.unical.campusreport.repository;

import it.unical.campusreport.entity.Zona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ZonaRepository extends JpaRepository<Zona, UUID> {
}

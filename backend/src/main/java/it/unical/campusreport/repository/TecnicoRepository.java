package it.unical.campusreport.repository;

import it.unical.campusreport.entity.Tecnico;
import it.unical.campusreport.entity.enums.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TecnicoRepository extends JpaRepository<Tecnico, UUID> {

    @Query("SELECT t FROM Tecnico t WHERE :categoria MEMBER OF t.specializzazioni AND t.attivo = true")
    List<Tecnico> findAttiviBySpecializzazione(@Param("categoria") Categoria categoria);
}

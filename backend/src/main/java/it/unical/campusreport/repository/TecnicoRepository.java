package it.unical.campusreport.repository;

import it.unical.campusreport.entity.Tecnico;
import it.unical.campusreport.entity.enums.Categoria;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TecnicoRepository extends JpaRepository<Tecnico, UUID> {

    @Query("SELECT t FROM Tecnico t WHERE :categoria MEMBER OF t.specializzazioni AND t.attivo = true")
    List<Tecnico> findAttiviBySpecializzazione(@Param("categoria") Categoria categoria);

    /**
     * Come {@link #findAttiviBySpecializzazione} ma con lock pessimistico WRITE.
     * Blocca le righe selezionate fino al commit della transazione corrente,
     * prevenendo la race condition nell'assegnazione concorrente dei tecnici.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tecnico t WHERE :categoria MEMBER OF t.specializzazioni AND t.attivo = true")
    List<Tecnico> findAttiviBySpecializzazioneWithLock(@Param("categoria") Categoria categoria);
}

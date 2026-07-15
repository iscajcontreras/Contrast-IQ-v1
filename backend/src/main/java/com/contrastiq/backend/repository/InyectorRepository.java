package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.Inyector;
import com.contrastiq.backend.model.enums.EstadoInyector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface InyectorRepository extends JpaRepository<Inyector, Long> {
    List<Inyector> findBySalaId(Long salaId);
    long countByEstado(EstadoInyector estado);

    // Usado por el job de "mantenimiento vencido a biomedica": inyectores
    // activos cuyo ultimo mantenimiento fue hace mas de X dias (o nunca
    // han tenido uno registrado).
    @Query("select i from Inyector i where i.estado = com.contrastiq.backend.model.enums.EstadoInyector.ACTIVO " +
           "and (i.fechaUltimoMantenimiento is null or i.fechaUltimoMantenimiento < :fechaLimite)")
    List<Inyector> conMantenimientoVencido(@Param("fechaLimite") LocalDate fechaLimite);
}

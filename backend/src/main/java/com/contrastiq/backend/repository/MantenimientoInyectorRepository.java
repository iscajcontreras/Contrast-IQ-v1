package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.MantenimientoInyector;
import com.contrastiq.backend.model.enums.TipoMantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MantenimientoInyectorRepository extends JpaRepository<MantenimientoInyector, Long> {
    List<MantenimientoInyector> findByInyector_IdOrderByFechaInicioDesc(Long inyectorId);
    Optional<MantenimientoInyector> findTopByInyector_IdAndTipoOrderByFechaInicioDesc(
            Long inyectorId, TipoMantenimiento tipo);
}

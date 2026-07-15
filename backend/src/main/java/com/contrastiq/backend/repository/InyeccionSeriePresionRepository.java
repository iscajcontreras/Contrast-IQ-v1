package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.InyeccionSeriePresion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InyeccionSeriePresionRepository extends JpaRepository<InyeccionSeriePresion, Long> {
    List<InyeccionSeriePresion> findByInyeccion_IdOrderByTiempoSegAsc(Long inyeccionId);
}

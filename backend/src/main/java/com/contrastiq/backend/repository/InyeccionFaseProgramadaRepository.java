package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.InyeccionFaseProgramada;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InyeccionFaseProgramadaRepository extends JpaRepository<InyeccionFaseProgramada, Long> {
    List<InyeccionFaseProgramada> findByInyeccion_IdOrderByOrdenFaseAsc(Long inyeccionId);
}

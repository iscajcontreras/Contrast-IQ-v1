package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.InyeccionFase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InyeccionFaseRepository extends JpaRepository<InyeccionFase, Long> {
    // Trazabilidad: todas las fases (y por lo tanto inyecciones/pacientes)
    // que usaron un lote especifico -- la consulta clave ante un recall.
    List<InyeccionFase> findByLoteAgenteIdOrderByInyeccion_FechaHoraInicioDesc(Long loteAgenteId);
}

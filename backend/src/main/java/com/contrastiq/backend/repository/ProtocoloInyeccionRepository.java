package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.ProtocoloInyeccion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProtocoloInyeccionRepository extends JpaRepository<ProtocoloInyeccion, Long> {
    List<ProtocoloInyeccion> findByActivoTrue();
    List<ProtocoloInyeccion> findByIdentificadorAnatomicoIdAndActivoTrue(Long identificadorAnatomicoId);
}

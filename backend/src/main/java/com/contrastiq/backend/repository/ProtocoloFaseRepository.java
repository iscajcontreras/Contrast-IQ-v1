package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.ProtocoloFase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProtocoloFaseRepository extends JpaRepository<ProtocoloFase, Long> {
    // Fases plantilla ("Planeado") de un protocolo, para el comparativo
    // Planeado/Programado/Real del detalle de inyeccion.
    List<ProtocoloFase> findByProtocolo_IdOrderByNumeroFaseAsc(Long protocoloId);
}

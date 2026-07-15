package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.EventoExtravasacion;
import com.contrastiq.backend.model.enums.EstadoEda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface EventoExtravasacionRepository
        extends JpaRepository<EventoExtravasacion, Long>, JpaSpecificationExecutor<EventoExtravasacion> {

    // Usado por el job programado de alertas en tiempo real: eventos
    // fuera de rango a los que aun no se les ha generado el push.
    List<EventoExtravasacion> findByEstadoEdaAndAlertaGeneradaFalse(EstadoEda estadoEda);

    // "Historial de reacciones por paciente"
    List<EventoExtravasacion> findByInyeccion_Paciente_IdOrderByFechaHoraDesc(Long pacienteId);
}

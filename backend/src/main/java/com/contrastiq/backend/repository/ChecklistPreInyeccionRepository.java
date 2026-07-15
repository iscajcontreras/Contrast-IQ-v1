package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.ChecklistPreInyeccion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChecklistPreInyeccionRepository extends JpaRepository<ChecklistPreInyeccion, Long> {
    List<ChecklistPreInyeccion> findByPaciente_IdOrderByFechaHoraDesc(Long pacienteId);
}

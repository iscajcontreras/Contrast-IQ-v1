package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

// Fix DEF-02 (QA julio 2026): la tabla auditoria existia en el esquema
// desde el inicio (tabla_afectada, registro_id, usuario, accion,
// fecha_hora, detalle_json) pero no tenia ningun repositorio ni
// servicio que la usara -- ver AuditoriaService para donde se escribe.
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
}

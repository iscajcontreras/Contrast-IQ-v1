package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.HistorialAcceso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HistorialAccesoRepository extends JpaRepository<HistorialAcceso, Long> {
    Page<HistorialAcceso> findByUsuario_IdOrderByFechaHoraDesc(Long usuarioId, Pageable pageable);
    Page<HistorialAcceso> findAllByOrderByFechaHoraDesc(Pageable pageable);

    // "Ultimo login" para la columna nueva de la tabla de usuarios:
    // el registro exitoso mas reciente de ese usuario.
    Optional<HistorialAcceso> findTopByUsuario_IdAndExitosoTrueOrderByFechaHoraDesc(Long usuarioId);
}

package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.HistorialAcceso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface HistorialAccesoRepository extends JpaRepository<HistorialAcceso, Long> {
    Page<HistorialAcceso> findByUsuario_IdOrderByFechaHoraDesc(Long usuarioId, Pageable pageable);
    Page<HistorialAcceso> findAllByOrderByFechaHoraDesc(Pageable pageable);

    // "Ultimo login" para la columna nueva de la tabla de usuarios:
    // el registro exitoso mas reciente de ese usuario.
    Optional<HistorialAcceso> findTopByUsuario_IdAndExitosoTrueOrderByFechaHoraDesc(Long usuarioId);

    // Fix DEF-01 (QA julio 2026): cuenta los intentos fallidos consecutivos
    // mas recientes de un email, usados por AuthService.login() para
    // decidir si debe bloquear la cuenta. Se filtra por fecha (ventana
    // deslizante) en vez de "desde el ultimo exitoso" para mantener la
    // consulta simple; un login exitoso limpia el contador explicitamente
    // en el servicio (bloqueadoHasta = null), asi que no hace falta que
    // esta consulta lo sepa.
    long countByEmailUsadoAndExitosoFalseAndFechaHoraAfter(String emailUsado, LocalDateTime desde);
}

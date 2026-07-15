package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.TokenRefresco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenRefrescoRepository extends JpaRepository<TokenRefresco, Long> {
    Optional<TokenRefresco> findByToken(String token);

    // "Usuario en linea" = tiene al menos un refresh token vigente (no
    // revocado, no expirado). Deliberadamente NO es presencia en tiempo
    // real (no hay websocket/heartbeat): significa "tiene una sesion
    // valida que podria estar en uso ahora mismo", sin estado extra en el
    // servidor y sin romperse si el backend corre en mas de una
    // instancia (ver notas del documento de referencia CEROGAS GPS).
    // Underscore explicito (Usuario_Id) para navegar la relacion, misma
    // convencion ya usada en HistorialAccesoRepository de este proyecto.
    boolean existsByUsuario_IdAndRevocadoFalseAndExpiraEnAfter(Long usuarioId, LocalDateTime ahora);
}

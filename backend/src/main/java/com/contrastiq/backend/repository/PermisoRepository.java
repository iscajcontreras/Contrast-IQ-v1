package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {
    List<Permiso> findAllByOrderByIdAsc();
    Optional<Permiso> findByCodigo(String codigo);
}

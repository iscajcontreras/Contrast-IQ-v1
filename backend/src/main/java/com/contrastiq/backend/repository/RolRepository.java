package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.Rol;
import com.contrastiq.backend.model.enums.NombreRol;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByNombre(NombreRol nombre);
}

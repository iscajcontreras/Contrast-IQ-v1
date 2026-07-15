package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModuloRepository extends JpaRepository<Modulo, Long> {
    List<Modulo> findAllByOrderByOrdenAsc();
    Optional<Modulo> findByCodigo(String codigo);
}

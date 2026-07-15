package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.Sala;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalaRepository extends JpaRepository<Sala, Long> {
    List<Sala> findBySedeIdAndActivoTrue(Long sedeId);
}

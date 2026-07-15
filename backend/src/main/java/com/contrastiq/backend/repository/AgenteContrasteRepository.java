package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.AgenteContraste;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AgenteContrasteRepository extends JpaRepository<AgenteContraste, Long> {
    List<AgenteContraste> findByActivoTrue();
}

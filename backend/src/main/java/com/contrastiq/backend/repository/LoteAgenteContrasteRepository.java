package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.LoteAgenteContraste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface LoteAgenteContrasteRepository
        extends JpaRepository<LoteAgenteContraste, Long>, JpaSpecificationExecutor<LoteAgenteContraste> {
    Optional<LoteAgenteContraste> findByNumeroLoteAndAgenteIdAndSedeId(String numeroLote, Long agenteId, Long sedeId);
}

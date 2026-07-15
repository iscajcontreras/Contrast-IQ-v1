package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.IdentificadorAnatomico;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentificadorAnatomicoRepository extends JpaRepository<IdentificadorAnatomico, Long> {
}

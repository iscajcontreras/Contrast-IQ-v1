package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long>, JpaSpecificationExecutor<Paciente> {
    Optional<Paciente> findByIdentificadorExterno(String identificadorExterno);
}

package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.TicketSoporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface TicketSoporteRepository
        extends JpaRepository<TicketSoporte, Long>, JpaSpecificationExecutor<TicketSoporte> {
    List<TicketSoporte> findByInyector_IdOrderByFechaCreacionDesc(Long inyectorId);
}

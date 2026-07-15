package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.LoteSincronizacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoteSincronizacionRepository extends JpaRepository<LoteSincronizacion, Long> {
    Page<LoteSincronizacion> findAllByOrderByFechaHoraDesc(Pageable pageable);
}

package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.InventarioAgente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface InventarioAgenteRepository extends JpaRepository<InventarioAgente, Long> {
    List<InventarioAgente> findBySedeId(Long sedeId);

    // Usado por el job de "aviso de stock bajo a farmacia"
    @Query("select i from InventarioAgente i where i.stockMl < i.stockMinimoMl")
    List<InventarioAgente> conStockBajo();
}

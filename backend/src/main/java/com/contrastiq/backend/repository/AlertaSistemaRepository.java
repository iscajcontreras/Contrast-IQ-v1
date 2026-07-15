package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.AlertaSistema;
import com.contrastiq.backend.model.enums.TipoAlerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface AlertaSistemaRepository extends JpaRepository<AlertaSistema, Long>, JpaSpecificationExecutor<AlertaSistema> {
    List<AlertaSistema> findByResueltaFalseOrderByFechaHoraDesc();

    // Usados por los jobs programados para no duplicar la misma alerta
    // dia tras dia mientras la condicion siga sin resolverse.
    boolean existsByTipoAndInyector_IdAndResueltaFalse(TipoAlerta tipo, Long inyectorId);
    boolean existsByTipoAndMensajeContainingAndResueltaFalse(TipoAlerta tipo, String fragmentoMensaje);
}

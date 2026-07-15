package com.contrastiq.backend.repository;

import com.contrastiq.backend.model.InyeccionSerieFlujo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InyeccionSerieFlujoRepository extends JpaRepository<InyeccionSerieFlujo, Long> {
    List<InyeccionSerieFlujo> findByInyeccion_IdOrderByTiempoSegAsc(Long inyeccionId);
}

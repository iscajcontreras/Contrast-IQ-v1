package com.contrastiq.backend.repository.spec;

import com.contrastiq.backend.dto.filtro.FiltroInyeccionDTO;
import com.contrastiq.backend.model.Inyeccion;
import com.contrastiq.backend.model.enums.EstadoInyeccion;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

// Construye el WHERE dinamico de /api/inyecciones a partir de los filtros
// que vienen del dashboard (rango de fechas, sala, inyector, protocolo,
// identificador anatomico, agente, estado). Cada filtro es opcional: si
// llega null simplemente no se agrega esa condicion.
public class InyeccionSpecification {

    private InyeccionSpecification() {
    }

    public static Specification<Inyeccion> conFiltros(FiltroInyeccionDTO filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (filtro.getFechaInicio() != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fechaHoraInicio"), filtro.getFechaInicio()));
            }
            if (filtro.getFechaFin() != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("fechaHoraInicio"), filtro.getFechaFin()));
            }
            if (filtro.getSedeId() != null) {
                predicados.add(cb.equal(root.get("inyector").get("sala").get("sede").get("id"), filtro.getSedeId()));
            }
            if (filtro.getSalaId() != null) {
                predicados.add(cb.equal(root.get("inyector").get("sala").get("id"), filtro.getSalaId()));
            }
            if (filtro.getInyectorId() != null) {
                predicados.add(cb.equal(root.get("inyector").get("id"), filtro.getInyectorId()));
            }
            if (filtro.getProtocoloId() != null) {
                predicados.add(cb.equal(root.get("protocolo").get("id"), filtro.getProtocoloId()));
            }
            if (filtro.getIdentificadorAnatomicoId() != null) {
                predicados.add(cb.equal(root.get("identificadorAnatomico").get("id"),
                        filtro.getIdentificadorAnatomicoId()));
            }
            if (filtro.getAgenteId() != null) {
                predicados.add(cb.equal(root.join("fases").get("agente").get("id"), filtro.getAgenteId()));
            }
            if (filtro.getEstado() != null) {
                predicados.add(cb.equal(root.get("estado"), EstadoInyeccion.valueOf(filtro.getEstado())));
            }
            if (filtro.getSoloConAlertaEda() != null && filtro.getSoloConAlertaEda()) {
                predicados.add(cb.equal(
                        root.join("eventosExtravasacion").get("estadoEda"),
                        com.contrastiq.backend.model.enums.EstadoEda.FUERA_DE_RANGO));
            }

            if (query != null) {
                query.distinct(true);
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}

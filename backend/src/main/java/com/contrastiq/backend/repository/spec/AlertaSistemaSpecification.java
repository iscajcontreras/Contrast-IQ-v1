package com.contrastiq.backend.repository.spec;

import com.contrastiq.backend.model.AlertaSistema;
import com.contrastiq.backend.model.enums.Severidad;
import com.contrastiq.backend.model.enums.TipoAlerta;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class AlertaSistemaSpecification {

    private AlertaSistemaSpecification() {
    }

    public static Specification<AlertaSistema> conFiltros(Boolean resuelta, String severidad, String tipo) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (resuelta != null) {
                predicados.add(cb.equal(root.get("resuelta"), resuelta));
            }
            if (severidad != null && !severidad.isBlank()) {
                predicados.add(cb.equal(root.get("severidad"), Severidad.valueOf(severidad)));
            }
            if (tipo != null && !tipo.isBlank()) {
                predicados.add(cb.equal(root.get("tipo"), TipoAlerta.valueOf(tipo)));
            }

            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}

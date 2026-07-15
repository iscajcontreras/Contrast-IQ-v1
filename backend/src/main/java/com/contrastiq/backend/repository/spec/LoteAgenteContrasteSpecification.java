package com.contrastiq.backend.repository.spec;

import com.contrastiq.backend.model.LoteAgenteContraste;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LoteAgenteContrasteSpecification {

    private LoteAgenteContrasteSpecification() {
    }

    public static Specification<LoteAgenteContraste> conFiltros(
            Long sedeId, Long agenteId, Boolean soloVigentes, Boolean proximosACaducar) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (sedeId != null) {
                predicados.add(cb.equal(root.get("sede").get("id"), sedeId));
            }
            if (agenteId != null) {
                predicados.add(cb.equal(root.get("agente").get("id"), agenteId));
            }
            if (Boolean.TRUE.equals(soloVigentes)) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fechaCaducidad"), LocalDate.now()));
            }
            if (Boolean.TRUE.equals(proximosACaducar)) {
                predicados.add(cb.between(root.get("fechaCaducidad"), LocalDate.now(), LocalDate.now().plusDays(30)));
            }

            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}

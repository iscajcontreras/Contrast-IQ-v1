package com.contrastiq.backend.repository.spec;

import com.contrastiq.backend.model.Inyeccion;
import com.contrastiq.backend.model.Paciente;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class PacienteSpecification {

    private PacienteSpecification() {
    }

    public static Specification<Paciente> conBusqueda(String busqueda) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (busqueda != null && !busqueda.isBlank()) {
                String like = "%" + busqueda.toLowerCase() + "%";
                predicados.add(cb.or(
                        cb.like(cb.lower(root.get("identificadorExterno")), like),
                        cb.like(cb.lower(root.get("nombreCompleto")), like)
                ));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    // Fix DEF-03 (QA julio 2026): a diferencia de Lotes, un Paciente NO
    // tiene sede_id propia (un mismo paciente puede recibir inyecciones
    // en mas de una sede a lo largo del tiempo) -- por eso la
    // restriccion de sede aqui no es "el paciente es de esa sede", sino
    // "el usuario solo puede ver pacientes con al menos una inyeccion
    // registrada en su propia sede" (EXISTS correlacionado). Es la
    // interpretacion mas defendible del esquema real; si el hospital
    // necesita un concepto mas estricto de "paciente asignado a una
    // sede", es un cambio de modelo de datos aparte.
    public static Specification<Paciente> conSedeRestriccion(Long sedeId) {
        return (root, query, cb) -> {
            if (sedeId == null) {
                return cb.conjunction();
            }
            Subquery<Long> subquery = query.subquery(Long.class);
            var inyeccionRoot = subquery.from(Inyeccion.class);
            subquery.select(inyeccionRoot.get("id"))
                    .where(cb.and(
                            cb.equal(inyeccionRoot.get("paciente"), root),
                            cb.equal(inyeccionRoot.get("inyector").get("sala").get("sede").get("id"), sedeId)
                    ));
            return cb.exists(subquery);
        };
    }
}

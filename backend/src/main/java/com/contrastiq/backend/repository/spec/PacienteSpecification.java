package com.contrastiq.backend.repository.spec;

import com.contrastiq.backend.model.Paciente;
import jakarta.persistence.criteria.Predicate;
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
}

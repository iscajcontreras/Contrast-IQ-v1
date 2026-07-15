package com.contrastiq.backend.repository.spec;

import com.contrastiq.backend.model.Usuario;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

// WHERE dinamico para GET /api/usuarios (pantalla de gestion de usuarios).
public class UsuarioSpecification {

    private UsuarioSpecification() {
    }

    public static Specification<Usuario> conFiltros(Long sedeId, Long rolId, Boolean activo, String busqueda) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (sedeId != null) {
                predicados.add(cb.equal(root.get("sede").get("id"), sedeId));
            }
            if (rolId != null) {
                predicados.add(cb.equal(root.get("rol").get("id"), rolId));
            }
            if (activo != null) {
                predicados.add(cb.equal(root.get("activo"), activo));
            }
            if (busqueda != null && !busqueda.isBlank()) {
                String like = "%" + busqueda.toLowerCase() + "%";
                predicados.add(cb.or(
                        cb.like(cb.lower(root.get("nombreCompleto")), like),
                        cb.like(cb.lower(root.get("email")), like)
                ));
            }

            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}

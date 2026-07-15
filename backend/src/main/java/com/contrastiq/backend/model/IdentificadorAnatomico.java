package com.contrastiq.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "identificadores_anatomicos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdentificadorAnatomico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(length = 255)
    private String descripcion;
}

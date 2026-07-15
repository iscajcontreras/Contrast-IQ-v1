package com.contrastiq.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Token opaco (no es un JWT) que el front cambia por un access token
// nuevo cuando el actual expira, sin pedirle contrasena otra vez al
// usuario. Se guarda en BD (no autocontenido como un JWT) a proposito:
// asi se puede revocar de verdad (cerrar sesion, o si se compromete),
// cosa que un JWT firmado no permite sin un blocklist aparte.
@Entity
@Table(name = "tokens_refresco")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRefresco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(nullable = false)
    @Builder.Default
    private Boolean revocado = false;

    @Column(name = "creado_en", updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}

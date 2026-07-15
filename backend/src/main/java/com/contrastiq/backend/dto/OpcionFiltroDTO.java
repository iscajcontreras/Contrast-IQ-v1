package com.contrastiq.backend.dto;

import lombok.*;

// Formato generico {id, etiqueta} para poblar cualquier <select> de filtro
// en Angular (salas, inyectores, protocolos, agentes, identificadores).
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpcionFiltroDTO {
    private Long id;
    private String etiqueta;
}

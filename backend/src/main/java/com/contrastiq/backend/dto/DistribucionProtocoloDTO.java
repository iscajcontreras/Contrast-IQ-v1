package com.contrastiq.backend.dto;

import lombok.*;

// Una barra del panel "Distribucion por protocolo" (identificador anatomico).
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DistribucionProtocoloDTO {
    private String identificadorAnatomico;
    private Long total;
    private Double porcentaje;
}

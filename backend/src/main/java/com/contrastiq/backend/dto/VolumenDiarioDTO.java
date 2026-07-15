package com.contrastiq.backend.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

// Un punto del grafico de barras "Uso de agente de contraste por dia".
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolumenDiarioDTO {
    private LocalDate fecha;
    private BigDecimal volumenMl;
    private Long totalInyecciones;
}

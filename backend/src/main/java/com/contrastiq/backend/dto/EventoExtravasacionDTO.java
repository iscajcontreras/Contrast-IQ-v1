package com.contrastiq.backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoExtravasacionDTO {
    private Long id;
    private Long inyeccionId;
    private LocalDateTime fechaHora;
    private String estadoEda;
    private Boolean revisado;
    private String sala;
    private String inyector;
    private String accionTomada;
}

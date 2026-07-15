package com.contrastiq.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ActualizarDosisRadiacionRequest {
    private BigDecimal ctdiVolMgy;
    private BigDecimal dlpMgyCm;
}

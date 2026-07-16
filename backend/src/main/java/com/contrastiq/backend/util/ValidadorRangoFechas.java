package com.contrastiq.backend.util;

import java.time.LocalDateTime;

// Fix DEF-04 (QA julio 2026): ningun endpoint que recibe un rango
// desde/hasta (Merma de insumos, Reportes ejecutivos) validaba que
// desde no fuera posterior a hasta -- un rango invertido no rompia
// nada (las consultas simplemente no encontraban filas), pero producia
// respuestas silenciosamente vacias/enganosas en vez de un 400 claro.
// IllegalArgumentException ya esta mapeada a HTTP 400 por
// ManejadorGlobalExcepciones, asi que no hace falta manejo adicional.
public final class ValidadorRangoFechas {

    private ValidadorRangoFechas() {
    }

    public static void validar(LocalDateTime desde, LocalDateTime hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a la fecha 'hasta'");
        }
    }
}

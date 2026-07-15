package com.contrastiq.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Habilita los jobs @Scheduled (alertas automaticas: EDA fuera de
// rango, mantenimiento vencido, stock bajo). Spring Boot no lo activa
// por defecto, hay que declararlo explicitamente.
@Configuration
@EnableScheduling
public class SchedulingConfig {
}

package com.contrastiq.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

// Habilita @PreAuthorize en los controllers/servicios (por ejemplo,
// restringir la gestion de usuarios solo al rol ADMIN). Spring Boot no
// activa esto automaticamente, hay que declararlo explicitamente.
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}

package com.contrastiq.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// Filtro de seguridad "por defecto": lo que quede fuera de /api/** (que
// ya protege ResourceServerConfig) y no sea parte del handshake de
// WebSocket. Ya no hay pantalla de login servida por este backend ni
// flujo OAuth2 con redirect (ver AuthController.login() y
// JwtSecurityConfig): el login vive en una llamada REST directa desde
// el front, no aqui.
@Configuration
@RequiredArgsConstructor
public class DefaultSecurityConfig {

    private final UserDetailsService usuarioDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @Order(3)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/error", "/ws/**").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        // Spring Security 6.3+ deprecó el constructor vacío y
        // setUserDetailsService(): ahora el UserDetailsService se pasa
        // directo al constructor. Ya no se usa para form-login (eso
        // desaparecio), pero UsuarioDetailsService se sigue registrando
        // como bean por si algo mas de Spring Security lo requiere.
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(usuarioDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}

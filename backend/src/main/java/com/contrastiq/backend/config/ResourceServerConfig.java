package com.contrastiq.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

// Protege /api/** exigiendo un access_token JWT valido (emitido por
// AuthService.login()/JwtSecurityConfig, ver esas clases) en el header
// Authorization: Bearer. Sin sesion de servidor (stateless), como
// corresponde a una API REST consumida por una SPA.
@Configuration
public class ResourceServerConfig {

    // Lista separada por comas, con patrones (soporta "*" como comodin).
    // Los valores por defecto ya cubren "de fabrica" el caso de correr
    // `ng serve --host 0.0.0.0` para probar desde otra maquina/celular en
    // la misma red local (192.168.x.x y 10.x.x.x son los rangos tipicos
    // de redes caseras/oficina), ademas de localhost/127.0.0.1 de siempre.
    // Si tu red usa otro rango (ej. 172.16.x.x), agregalo aqui.
    @Value("${app.cors.origenes-permitidos:http://localhost:4200,http://127.0.0.1:4200,"
            + "http://192.168.*.*:4200,http://10.*.*.*:4200}")
    private String origenesPermitidos;

    @Bean
    @Order(2)
    public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // El preflight de CORS (OPTIONS) debe pasar siempre, sin importar
                        // la ruta: si no se permite explicitamente aqui, Spring Security
                        // puede bloquearlo con 403 antes de que el filtro de CORS lo
                        // procese -- el navegador lo ve como "CORS error" aunque el
                        // problema real es de autorizacion.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // login, refresh y logout son publicos (asi es como se consigue
                        // el token en primer lugar); igual que registro/recuperacion.
                        .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/logout",
                                "/api/auth/registro", "/api/auth/olvidar-password",
                                "/api/auth/restablecer-password").permitAll()
                        // el resto de la API exige un access_token valido
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    // BUGFIX (validacion Prioridad 1): por defecto Spring Security busca
    // el claim "scope"/"scp" para armar las authorities del JWT. Nuestro
    // token (ver AuthService.emitirTokens()) manda el rol del usuario en
    // el claim "roles" (ya con el prefijo ROLE_), asi que hace falta este
    // converter explicito -- sin el, hasRole('ADMIN') nunca encontraba
    // nada que evaluar y devolvia 403 para cualquier usuario, incluido un
    // ADMIN real (afectaba UsuarioController e HistorialAccesoController).
    private org.springframework.core.convert.converter.Converter<org.springframework.security.oauth2.jwt.Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();
        // setAllowedOriginPatterns (no setAllowedOrigins): es la variante
        // que entiende comodines ("*") dentro del patron, necesaria para
        // permitir cualquier IP dentro de 192.168.x.x/10.x.x.x sin tener
        // que listar la IP exacta de cada maquina de prueba.
        configuracion.setAllowedOriginPatterns(List.of(origenesPermitidos.split(",")));
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuracion.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuracion);
        return source;
    }
}

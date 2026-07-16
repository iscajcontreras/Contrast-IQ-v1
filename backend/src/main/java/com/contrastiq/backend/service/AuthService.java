package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.LoginRequest;
import com.contrastiq.backend.dto.OlvidarPasswordRequest;
import com.contrastiq.backend.dto.RefrescarTokenRequest;
import com.contrastiq.backend.dto.RegistroRequest;
import com.contrastiq.backend.dto.RestablecerPasswordRequest;
import com.contrastiq.backend.dto.TokenResponse;
import com.contrastiq.backend.model.TokenRecuperacionPassword;
import com.contrastiq.backend.model.TokenRefresco;
import com.contrastiq.backend.model.Usuario;
import com.contrastiq.backend.model.enums.NombreRol;
import com.contrastiq.backend.model.enums.ProveedorAutenticacion;
import com.contrastiq.backend.repository.RolRepository;
import com.contrastiq.backend.repository.TokenRecuperacionPasswordRepository;
import com.contrastiq.backend.repository.TokenRefrescoRepository;
import com.contrastiq.backend.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// "Login directo con JWT Bearer": valida usuario/contrasena aqui mismo y
// firma el access token con la llave RSA de JwtSecurityConfig -- sin
// redirect a un Authorization Server aparte, sin PKCE, sin doble
// pantalla de login. El refresh token es opaco (no JWT) y vive en BD
// (tokens_refresco) para poder revocarse.
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final TokenRecuperacionPasswordRepository tokenRepository;
    private final TokenRefrescoRepository tokenRefrescoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final HistorialAccesoService historialAccesoService;
    private final JwtEncoder jwtEncoder;

    @Value("${app.frontend.url-base:http://localhost:4200}")
    private String frontendUrlBase;

    @Value("${app.jwt.issuer:contrast-iq}")
    private String issuer;

    @Value("${app.jwt.access-token-minutos:60}")
    private long accessTokenMinutos;

    @Value("${app.jwt.refresh-token-dias:7}")
    private long refreshTokenDias;

    // Fix DEF-01 (QA julio 2026): bloqueo temporal de cuenta tras varios
    // intentos fallidos consecutivos (fuerza bruta). historial_accesos ya
    // registraba cada intento -- no habia ningun bloqueo que lo usara.
    private static final int MAX_INTENTOS_FALLIDOS = 5;
    private static final long VENTANA_INTENTOS_MINUTOS = 15;
    private static final long BLOQUEO_MINUTOS = 15;

    // --- Sign up ---
    @Transactional
    public void registrar(RegistroRequest request) {
        usuarioRepository.findByEmail(request.getEmail()).ifPresent(u -> {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo");
        });

        Usuario usuario = Usuario.builder()
                .nombreCompleto(request.getNombreCompleto())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .proveedor(ProveedorAutenticacion.LOCAL)
                .emailVerificado(false)
                // rol por defecto: acceso de solo lectura hasta que un
                // administrador le asigne un rol operativo (TECNICO, etc.)
                .rol(rolRepository.findByNombre(NombreRol.VISUALIZADOR)
                        .orElseThrow(() -> new IllegalStateException("Rol VISUALIZADOR no existe")))
                .activo(true)
                .build();

        usuarioRepository.save(usuario);
    }

    // --- Login ---
    @Transactional
    public TokenResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail()).orElse(null);

        // Fix DEF-01 (QA julio 2026): si la cuenta esta bloqueada y el
        // bloqueo sigue vigente, rechazar sin ni siquiera comparar la
        // contrasena. Se registra igual en el historial (como fallido)
        // para conservar la traza completa de intentos.
        if (usuario != null && usuario.getBloqueadoHasta() != null
                && usuario.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
            historialAccesoService.registrar(request.getEmail(), false, ProveedorAutenticacion.LOCAL,
                    httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
            throw new BadCredentialsException(
                    "Cuenta bloqueada temporalmente por multiples intentos fallidos. Intenta de nuevo mas tarde.");
        }

        boolean credencialesValidas = usuario != null
                && usuario.getPasswordHash() != null
                && Boolean.TRUE.equals(usuario.getActivo())
                && passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash());

        historialAccesoService.registrar(request.getEmail(), credencialesValidas, ProveedorAutenticacion.LOCAL,
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));

        if (!credencialesValidas) {
            // Fix DEF-01 (QA julio 2026): si ya hay MAX_INTENTOS_FALLIDOS
            // (incluyendo el que se acaba de registrar) dentro de la
            // ventana, bloquear la cuenta. Solo aplica si el email
            // corresponde a una cuenta real (usuario != null) -- un
            // atacante probando emails inexistentes no puede "bloquear"
            // nada que no exista.
            if (usuario != null) {
                long fallidosRecientes = historialAccesoService.contarFallidosRecientes(
                        request.getEmail(), LocalDateTime.now().minusMinutes(VENTANA_INTENTOS_MINUTOS));
                if (fallidosRecientes >= MAX_INTENTOS_FALLIDOS) {
                    usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(BLOQUEO_MINUTOS));
                    usuarioRepository.save(usuario);
                }
            }
            throw new BadCredentialsException("Correo o contrasena incorrectos");
        }

        // Login exitoso: si venia de un bloqueo ya expirado, limpiar el
        // campo explicitamente en vez de dejarlo con una fecha pasada.
        if (usuario.getBloqueadoHasta() != null) {
            usuario.setBloqueadoHasta(null);
            usuarioRepository.save(usuario);
        }

        return emitirTokens(usuario);
    }

    // --- Refrescar access token ---
    @Transactional
    public TokenResponse refrescar(RefrescarTokenRequest request) {
        TokenRefresco tokenRefresco = tokenRefrescoRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BadCredentialsException("Refresh token invalido"));

        if (Boolean.TRUE.equals(tokenRefresco.getRevocado())) {
            throw new BadCredentialsException("Refresh token revocado");
        }
        if (tokenRefresco.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Refresh token expirado, inicia sesion de nuevo");
        }

        // Se consume: cada refresh emite un refresh token nuevo y revoca
        // el anterior (evita que un refresh token robado siga sirviendo
        // en paralelo al legitimo indefinidamente).
        tokenRefresco.setRevocado(true);
        tokenRefrescoRepository.save(tokenRefresco);

        return emitirTokens(tokenRefresco.getUsuario());
    }

    // --- Logout ---
    @Transactional
    public void cerrarSesion(RefrescarTokenRequest request) {
        tokenRefrescoRepository.findByToken(request.getRefreshToken()).ifPresent(t -> {
            t.setRevocado(true);
            tokenRefrescoRepository.save(t);
        });
    }

    private TokenResponse emitirTokens(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expiraEn = ahora.plusSeconds(accessTokenMinutos * 60);

        // BUGFIX (validacion Prioridad 1): el token no llevaba el rol del
        // usuario como claim, por lo que ResourceServerConfig nunca tenia
        // con que resolver hasRole('ADMIN') -- todo @PreAuthorize devolvia
        // 403 sin importar el rol real (afectaba UsuarioController e
        // HistorialAccesoController). Se agrega el claim "roles" con el
        // prefijo ROLE_ que espera Spring Security, y se lee en
        // ResourceServerConfig.jwtAuthenticationConverter().
        String rolConPrefijo = "ROLE_" + usuario.getRol().getNombre().name();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(usuario.getEmail())
                .issuedAt(ahora)
                .expiresAt(expiraEn)
                .claim("roles", List.of(rolConPrefijo))
                .build();

        String accessToken = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)
        ).getTokenValue();

        String refreshTokenValor = UUID.randomUUID().toString();
        TokenRefresco tokenRefresco = TokenRefresco.builder()
                .usuario(usuario)
                .token(refreshTokenValor)
                .expiraEn(LocalDateTime.now().plusDays(refreshTokenDias))
                .revocado(false)
                .build();
        tokenRefrescoRepository.save(tokenRefresco);

        return new TokenResponse(accessToken, refreshTokenValor, accessTokenMinutos * 60);
    }

    // --- Forgot password ---
    @Transactional
    public void solicitarRecuperacion(OlvidarPasswordRequest request) {
        // Nota deliberada de seguridad: no revelamos si el correo existe o
        // no (evita enumeracion de cuentas). Siempre "parece" exitoso.
        usuarioRepository.findByEmail(request.getEmail()).ifPresent(usuario -> {
            String token = UUID.randomUUID().toString();
            TokenRecuperacionPassword tokenRecuperacion = TokenRecuperacionPassword.builder()
                    .usuario(usuario)
                    .token(token)
                    .expiraEn(LocalDateTime.now().plusMinutes(30))
                    .usado(false)
                    .build();
            tokenRepository.save(tokenRecuperacion);

            String enlace = frontendUrlBase + "/auth/reset-password?token=" + token;
            emailService.enviarEnlaceRecuperacion(usuario.getEmail(), enlace);
        });
    }

    // --- Reset password ---
    @Transactional
    public void restablecerPassword(RestablecerPasswordRequest request) {
        TokenRecuperacionPassword tokenRecuperacion = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("El enlace de recuperacion no es valido"));

        if (Boolean.TRUE.equals(tokenRecuperacion.getUsado())) {
            throw new IllegalArgumentException("Este enlace ya fue utilizado");
        }
        if (tokenRecuperacion.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El enlace de recuperacion expiro, solicita uno nuevo");
        }

        Usuario usuario = tokenRecuperacion.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(request.getNuevaPassword()));
        usuarioRepository.save(usuario);

        tokenRecuperacion.setUsado(true);
        tokenRepository.save(tokenRecuperacion);
    }
}

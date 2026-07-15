package com.contrastiq.backend.service;

import com.contrastiq.backend.model.Usuario;
import com.contrastiq.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Puente entre Spring Security y la tabla `usuarios`. Registrado como
// bean de UserDetailsService por si algo mas de Spring Security lo
// necesita -- el login real ya no pasa por aqui (ver AuthService.login,
// que valida la contrasena directo con PasswordEncoder.matches()).
@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    // @Transactional es necesario aqui: Usuario.rol es LAZY, y sin una
    // sesion de Hibernate abierta durante toda la ejecucion del metodo,
    // usuario.getRol().getNombre() lanza LazyInitializationException
    // apenas el repositorio cierra la sesion original.
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales invalidas"));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new UsernameNotFoundException("Credenciales invalidas");
        }
        if (usuario.getPasswordHash() == null) {
            // Cuenta creada solo para Google: no tiene password local valida
            throw new UsernameNotFoundException("Credenciales invalidas");
        }

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre())))
                .disabled(!usuario.getActivo())
                .build();
    }
}

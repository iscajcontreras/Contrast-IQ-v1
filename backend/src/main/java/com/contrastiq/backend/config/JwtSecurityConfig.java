package com.contrastiq.backend.config;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;

// Llave RSA compartida para firmar/validar los access tokens que este
// backend emite. Reemplaza al viejo AuthorizationServerConfig (OAuth 2.1
// completo con /oauth2/authorize + PKCE + redirect): para una SPA de
// primera parte que solo habla con su propio backend, esa capa de
// indireccion no aportaba nada y solo hacia el login mas incomodo (dos
// pantallas, dos dominios). Ahora AuthController.login() valida el
// usuario/contrasena directo y firma el JWT aqui mismo.
//
// PERSISTENCIA (Prioridad 2 de PROXIMOS_PASOS.md, resuelta aqui): antes
// la llave se generaba en memoria en cada arranque, asi que reiniciar el
// backend (deploy, crash, mantenimiento) invalidaba TODAS las sesiones
// activas sin aviso -- documentado como limitacion conocida. Ahora la
// llave se guarda en disco (DER crudo, PKCS8 para la privada y X509 para
// la publica) la primera vez que se genera, y en arranques siguientes se
// lee de ahi en vez de generar una nueva. Un reinicio del backend ya NO
// invalida los access tokens emitidos antes, mientras sigan dentro de su
// ventana de expiracion.
//
// Ubicacion configurable via app.jwt.llave-directorio (default ./keys,
// relativo al directorio de trabajo del proceso). Ese directorio NO debe
// versionarse en git (agregar "keys/" al .gitignore del backend): es
// material criptografico real, equivalente a una contrasena.
//
// IMPORTANTE: el JwtEncoder (para firmar) y el JwtDecoder (para validar)
// se arman los DOS a partir del mismo KeyPair bean de aqui abajo -- nunca
// intentar "recuperar" la llave publica desde el JWKSource despues (esa
// ruta requiere anadir dependencias extra de Nimbus y es innecesariamente
// fragil frente a cambios de version de la libreria).
@Configuration
public class JwtSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtSecurityConfig.class);

    @Value("${app.jwt.llave-directorio:./keys}")
    private String llaveDirectorio;

    @Bean
    public KeyPair rsaKeyPair() {
        Path directorio = Path.of(llaveDirectorio);
        Path privadaPath = directorio.resolve("jwt-rsa-privada.der");
        Path publicaPath = directorio.resolve("jwt-rsa-publica.der");

        if (Files.exists(privadaPath) && Files.exists(publicaPath)) {
            try {
                KeyPair keyPair = cargarDesdeDisco(privadaPath, publicaPath);
                log.info("Llave RSA del JWT cargada desde {} (reinicio del backend NO invalida sesiones previas)",
                        directorio.toAbsolutePath());
                return keyPair;
            } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
                throw new IllegalStateException(
                        "No se pudo leer la llave RSA existente en " + directorio.toAbsolutePath()
                                + " -- si el archivo esta corrupto, borra el directorio para que se regenere"
                                + " (esto invalidara las sesiones activas)", ex);
            }
        }

        log.warn("No se encontro una llave RSA en {} -- generando una nueva y guardandola ahi "
                        + "(solo debería pasar la primera vez que arranca el backend en este ambiente)",
                directorio.toAbsolutePath());
        try {
            KeyPair keyPair = generarNueva();
            guardarEnDisco(directorio, privadaPath, publicaPath, keyPair);
            return keyPair;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar/guardar el par de llaves RSA", ex);
        }
    }

    private KeyPair generarNueva() throws NoSuchAlgorithmException {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        return generador.generateKeyPair();
    }

    private void guardarEnDisco(Path directorio, Path privadaPath, Path publicaPath, KeyPair keyPair)
            throws IOException {
        Files.createDirectories(directorio);
        Files.write(privadaPath, keyPair.getPrivate().getEncoded());
        Files.write(publicaPath, keyPair.getPublic().getEncoded());
        log.info("Llave RSA del JWT generada y guardada en {}", directorio.toAbsolutePath());
    }

    private KeyPair cargarDesdeDisco(Path privadaPath, Path publicaPath)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        byte[] privadaBytes = Files.readAllBytes(privadaPath);
        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory
                .generatePrivate(new PKCS8EncodedKeySpec(privadaBytes));

        byte[] publicaBytes = Files.readAllBytes(publicaPath);
        RSAPublicKey publicKey = (RSAPublicKey) keyFactory
                .generatePublic(new X509EncodedKeySpec(publicaBytes));

        return new KeyPair(publicKey, privateKey);
    }

    @Bean
    public JwtEncoder jwtEncoder(KeyPair rsaKeyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) rsaKeyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) rsaKeyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new com.nimbusds.jose.jwk.JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(KeyPair rsaKeyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) rsaKeyPair.getPublic();
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }
}

package com.contrastiq.backend.controller.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class ManejadorGlobalExcepciones {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("timestamp", LocalDateTime.now().toString());
        cuerpo.put("mensaje", "Datos invalidos");
        cuerpo.put("errores", errores);
        return ResponseEntity.badRequest().body(cuerpo);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> manejarArgumentoInvalido(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(cuerpoError(ex.getMessage()));
    }

    // Login (email/password incorrectos) o refresh token invalido/expirado
    // (ver AuthService) -- 401, no 500.
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> manejarCredencialesInvalidas(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(cuerpoError(ex.getMessage()));
    }

    // Usuario autenticado pero sin el rol/permiso requerido (@PreAuthorize) -- 403.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> manejarAccesoDenegado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(cuerpoError("No tienes permiso para realizar esta accion"));
    }

    // Captura generica al final: registrar el stack trace completo con
    // log.error() ANTES de responder es la correccion de un bug real
    // encontrado en CEROGAS GPS -- sin esto, un 500 no dejaba ningun
    // rastro en la terminal del backend, haciendo imposible diagnosticar
    // el problema despues del hecho.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> manejarError(Exception ex) {
        log.error("Error no controlado atendiendo la peticion", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(cuerpoError(ex.getMessage()));
    }

    private Map<String, Object> cuerpoError(String mensaje) {
        return Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "mensaje", mensaje != null ? mensaje : "Error inesperado"
        );
    }
}

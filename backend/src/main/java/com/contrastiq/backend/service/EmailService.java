package com.contrastiq.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Envio de correo para el flujo de "olvide mi contrasena". Mientras
// app.correo.envio-habilitado=false (valor por defecto de este proyecto,
// porque no configuramos credenciales SMTP reales), esto NO envia correo:
// solo deja el enlace en el log para que puedas copiarlo y probar el
// flujo completo sin depender de un servidor de correo real.
//
// Para activarlo de verdad: configura spring.mail.* en
// application.properties con credenciales SMTP validas y cambia
// app.correo.envio-habilitado=true.
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.correo.envio-habilitado:false}")
    private boolean envioHabilitado;

    @Value("${app.correo.remitente:no-responder@hospital-contraste.mx}")
    private String remitente;

    public void enviarEnlaceRecuperacion(String destinatario, String enlace) {
        if (!envioHabilitado) {
            log.info("=== [EMAIL SIMULADO] Recuperacion de contrasena ===");
            log.info("Para: {}", destinatario);
            log.info("Enlace: {}", enlace);
            log.info("(app.correo.envio-habilitado=false: no se envio correo real)");
            return;
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject("Recupera tu contrasena - Inyectores de contraste");
        mensaje.setText(
                "Recibimos una solicitud para restablecer tu contrasena.\n\n" +
                "Si fuiste tu, entra a este enlace (valido por 30 minutos):\n" + enlace + "\n\n" +
                "Si no solicitaste esto, ignora este correo."
        );
        mailSender.send(mensaje);
    }
}

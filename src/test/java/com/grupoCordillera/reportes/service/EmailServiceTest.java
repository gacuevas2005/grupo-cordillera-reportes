package com.grupoCordillera.reportes.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void debeEnviarCorreoConAdjuntoExitosamente() {
        // Arrange (Preparar)
        String destinatario = "gerente@cordillera.cl";
        byte[] pdfFalso = new byte[]{1, 2, 3}; // Simulamos un PDF
        String periodo = "2026-06";

        // LA SOLUCIÓN: Creamos una sesión vacía y un MimeMessage REAL
        Session session = Session.getInstance(new Properties());
        MimeMessage mensajeRealVacio = new MimeMessage(session);

        // Le decimos a nuestro mock del mailSender que devuelva este mensaje real
        when(mailSender.createMimeMessage()).thenReturn(mensajeRealVacio);

        // Act (Actuar)
        emailService.enviarReporteConAdjunto(destinatario, pdfFalso, periodo);

        // Assert (Comprobar)
        // Verificamos que el mailSender intentó enviarlo exactamente 1 vez
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
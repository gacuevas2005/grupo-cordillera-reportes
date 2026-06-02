package com.grupoCordillera.reportes.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void enviarReporteConAdjunto(String destinatario, byte[] pdfBytes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // El 'true' indica que es un correo multipart (con archivos adjuntos)
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(destinatario);
            helper.setSubject("Tu Reporte de Grupo Cordillera");
            helper.setText("Hola,\n\nAdjunto encontrarás el último reporte de cumplimiento generado por el sistema.\n\nSaludos,\nEquipo TI.");

            // Adjuntamos el archivo en memoria y le damos un nombre
            helper.addAttachment("Reporte_Cordillera.pdf", new ByteArrayResource(pdfBytes));

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar el correo", e);
        }
    }
}
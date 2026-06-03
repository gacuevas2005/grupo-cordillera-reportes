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

    public void enviarReporteConAdjunto(String destinatario, byte[] pdfBytes, String periodo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(destinatario);
            // Hacemos que el Asunto sea dinámico
            helper.setSubject("Tu Reporte de Grupo Cordillera - " + periodo.toUpperCase());
            helper.setText("Hola,\n\nAdjunto encontrarás el último reporte de cumplimiento " + periodo.toLowerCase() + " generado por el sistema.\n\nSaludos,\nEquipo TI.");

            // Hacemos que el nombre del archivo adjunto sea dinámico
            helper.addAttachment("Reporte_Cordillera_" + periodo.toUpperCase() + ".pdf", new ByteArrayResource(pdfBytes));

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar el correo", e);
        }
    }
}
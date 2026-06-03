package com.grupoCordillera.reportes.controller;


import com.grupoCordillera.reportes.dto.ReporteCumplimientoDto;
import com.grupoCordillera.reportes.service.EmailService;
import com.grupoCordillera.reportes.service.PdfService;
import com.grupoCordillera.reportes.service.ReporteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    @Autowired
    private ReporteService reporteService; // El que armamos antes que cruza Ventas y KPIs

    @Autowired
    private PdfService pdfService;

    @Autowired
    private EmailService emailService;

    // Endpoint 1: Descargar el PDF directamente (ideal para un botón en React)
    @GetMapping("/descargar")
    public ResponseEntity<byte[]> descargarPdf(
            @RequestParam Long kpiId,
            @RequestParam Long sucursalId,
            // Agregamos el nuevo parámetro, con "MENSUAL" por defecto por si no lo envían
            @RequestParam(defaultValue = "MENSUAL") String periodo) {

        // Pasamos el nuevo parámetro al servicio
        ReporteCumplimientoDto reporte = reporteService.generarReporteDeCumplimiento(kpiId, sucursalId, periodo);
        byte[] pdfBytes = pdfService.generarPdfDeCumplimiento(reporte);

        return ResponseEntity.ok()
                // Le decimos al navegador que esto es un PDF y debe forzar la descarga
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // Endpoint 2: Enviar el PDF por correo
    @PostMapping("/enviar")
    public ResponseEntity<String> enviarPdfPorCorreo(
            @RequestParam Long kpiId,
            @RequestParam Long sucursalId,
            @RequestParam String correoDestino,
            @RequestParam(defaultValue = "MENSUAL") String periodo) {

        // 1. Generar los datos
        ReporteCumplimientoDto reporte = reporteService.generarReporteDeCumplimiento(kpiId, sucursalId, periodo);

        // 2. Crear el PDF
        byte[] pdfBytes = pdfService.generarPdfDeCumplimiento(reporte);

        // 3. Enviar el correo usando tu EmailService
        emailService.enviarReporteConAdjunto(correoDestino, pdfBytes, periodo);

        return ResponseEntity.ok("Reporte enviado con éxito a " + correoDestino);
    }
}
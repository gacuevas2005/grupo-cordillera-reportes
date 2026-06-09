package com.grupoCordillera.reportes.controller;

import com.grupoCordillera.reportes.dto.HistorialResumenDto;
import com.grupoCordillera.reportes.dto.ReporteCumplimientoDto;
import com.grupoCordillera.reportes.service.EmailService;
import com.grupoCordillera.reportes.service.PdfService;
import com.grupoCordillera.reportes.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Gestión de Reportes", description = "Endpoints para generar PDFs, enviar correos y consultar el historial de Grupo Cordillera")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private EmailService emailService;

    // Endpoint 1: Descargar el PDF directamente
    @Operation(summary = "Descargar reporte en formato PDF", description = "Genera un reporte de cumplimiento cruzando datos de KPIs y Ventas, lo guarda en el historial y retorna el archivo binario descargable.")
    @GetMapping("/descargar")
    public ResponseEntity<byte[]> descargarPdf(
            @Parameter(description = "ID del KPI a evaluar", example = "1") @RequestParam Long kpiId,
            @Parameter(description = "ID de la Sucursal", example = "1") @RequestParam Long sucursalId,
            @Parameter(description = "Filtro de tiempo (SEMANAL, MENSUAL, ANUAL)", example = "MENSUAL") @RequestParam(defaultValue = "MENSUAL") String periodo) {

        ReporteCumplimientoDto reporte = reporteService.generarReporteDeCumplimiento(kpiId, sucursalId, periodo);
        byte[] pdfBytes = pdfService.generarPdfDeCumplimiento(reporte);
        reporteService.guardarEnHistorial(kpiId, reporte.getNombreKpi(), periodo, reporte.getVentasReales(), reporte.getEstado(), pdfBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // Endpoint 2: Enviar el PDF por correo
    @Operation(summary = "Enviar reporte PDF por correo electrónico", description = "Genera el reporte, lo guarda en el historial y lo envía como archivo adjunto a la dirección de correo especificada.")
    @PostMapping("/enviar")
    public ResponseEntity<String> enviarPdfPorCorreo(
            @Parameter(description = "ID del KPI a evaluar", example = "1") @RequestParam Long kpiId,
            @Parameter(description = "ID de la Sucursal", example = "1") @RequestParam Long sucursalId,
            @Parameter(description = "Correo electrónico del destinatario", example = "gerente@cordillera.cl") @RequestParam String correoDestino,
            @Parameter(description = "Filtro de tiempo (SEMANAL, MENSUAL, ANUAL)", example = "MENSUAL") @RequestParam(defaultValue = "MENSUAL") String periodo) {

        ReporteCumplimientoDto reporte = reporteService.generarReporteDeCumplimiento(kpiId, sucursalId, periodo);
        byte[] pdfBytes = pdfService.generarPdfDeCumplimiento(reporte);

        reporteService.guardarEnHistorial(kpiId, reporte.getNombreKpi(), periodo, reporte.getVentasReales(), reporte.getEstado(), pdfBytes);
        emailService.enviarReporteConAdjunto(correoDestino, pdfBytes, periodo);

        return ResponseEntity.ok("Reporte enviado con éxito a " + correoDestino);
    }

    // Endpoint 3: Ver historial
    @Operation(summary = "Obtener el historial de reportes", description = "Retorna una lista con el resumen de todos los reportes generados previamente en el sistema.")
    @GetMapping("/historial")
    public ResponseEntity<List<HistorialResumenDto>> verHistorial() {
        List<HistorialResumenDto> lista = reporteService.listarHistorial();
        return ResponseEntity.ok(lista);
    }

    // Endpoint 4: Descargar un PDF antiguo
    @Operation(summary = "Descargar reporte histórico", description = "Busca un reporte en la base de datos mediante su ID y retorna el archivo PDF original.")
    @GetMapping("/historial/{id}/descargar")
    public ResponseEntity<byte[]> descargarHistorial(
            @Parameter(description = "ID del registro en el historial", example = "5") @PathVariable Long id) {

        byte[] pdfBytes = reporteService.descargarPdfHistorico(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Reporte_Historico_" + id + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    // Endpoint 5: Previsualizar el PDF (sin guardar)
    @Operation(summary = "Previsualizar reporte PDF", description = "Genera el reporte al vuelo para visualización en el navegador. NO guarda el registro en el historial.")
    @GetMapping("/previsualizar")
    public ResponseEntity<byte[]> previsualizarPdf(
            @Parameter(description = "ID del KPI a evaluar", example = "1") @RequestParam Long kpiId,
            @Parameter(description = "ID de la Sucursal", example = "1") @RequestParam Long sucursalId,
            @Parameter(description = "Filtro de tiempo (SEMANAL, MENSUAL, ANUAL)", example = "MENSUAL") @RequestParam(defaultValue = "MENSUAL") String periodo) {

        ReporteCumplimientoDto reporte = reporteService.generarReporteDeCumplimiento(kpiId, sucursalId, periodo);
        byte[] pdfBytes = pdfService.generarPdfDeCumplimiento(reporte);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
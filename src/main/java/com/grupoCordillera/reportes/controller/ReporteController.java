package com.grupoCordillera.reportes.controller;

import com.grupoCordillera.reportes.service.ReporteService;
import com.grupoCordillera.reportes.service.PdfService;
import com.grupoCordillera.reportes.service.EmailService;
import com.grupoCordillera.reportes.dto.ReporteCumplimientoDto;
import com.grupoCordillera.reportes.dto.HistorialResumenDto;

// Importaciones de Swagger / OpenAPI
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reportes", description = "API para la generación, gestión y envío de reportes PDF de cumplimiento de KPIs")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private EmailService emailService;

    // 1. Obtener historial de reportes con seguridad de sucursal
    @Operation(
            summary = "Obtener historial de reportes",
            description = "Devuelve una lista con el resumen de los reportes generados previamente. Aplica aislamiento multi-tenancy según el rol del usuario."
    )
    @ApiResponse(responseCode = "200", description = "Lista de historial devuelta exitosamente")
    @GetMapping("/historial")
    public ResponseEntity<List<HistorialResumenDto>> obtenerHistorial(
            @Parameter(description = "Rol del usuario inyectado por el BFF") @RequestHeader(value = "X-User-Role", required = false) String rol,
            @Parameter(description = "ID de la sucursal del usuario inyectado por el BFF") @RequestHeader(value = "X-Sucursal-Id", required = false) Long sucursalAutenticada) {

        if (rol == null || rol.trim().isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        List<HistorialResumenDto> historial = reporteService.listarHistorialPorSeguridad(rol, sucursalAutenticada);
        return ResponseEntity.ok(historial);
    }

    // 2. Descargar un reporte antiguo desde la base de datos (binario PDF)
    @Operation(
            summary = "Descargar reporte histórico",
            description = "Recupera un archivo PDF binario almacenado en la base de datos usando su ID del historial."
    )
    @ApiResponse(responseCode = "200", description = "Archivo PDF descargado exitosamente")
    @ApiResponse(responseCode = "404", description = "Reporte histórico no encontrado")
    @GetMapping("/historial/{id}/descargar")
    public ResponseEntity<byte[]> descargarReporteAntiguo(
            @Parameter(description = "ID único del reporte en el historial") @PathVariable Long id) {
        try {
            byte[] pdfBytes = reporteService.descargarPdfHistorico(id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Reporte_Historico_" + id + ".pdf");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // 3. Generar, guardar en historial y descargar reporte nuevo en tiempo real
    @Operation(
            summary = "Generar y descargar nuevo reporte",
            description = "Calcula el cumplimiento en tiempo real, compila un PDF, lo registra en el historial y fuerza su descarga en el cliente."
    )
    @ApiResponse(responseCode = "200", description = "Reporte generado y descargado exitosamente")
    @GetMapping("/descargar")
    public ResponseEntity<?> generarYDescargarReporte(
            @Parameter(description = "ID del KPI a evaluar") @RequestParam(required = false) Long kpiId,
            @Parameter(description = "ID de la sucursal (solo aplicable para ADMIN)") @RequestParam(required = false) Long sucursalId,
            @Parameter(description = "Periodo de evaluación (ej. MENSUAL, ANUAL)") @RequestParam(required = false) String periodo,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", required = false) String rol,
            @Parameter(hidden = true) @RequestHeader(value = "X-Sucursal-Id", required = false) Long sucursalAutenticada) {

        Long kpiFinal = (kpiId != null && kpiId != 0) ? kpiId : 1L;
        String periodoFinal = (periodo != null && !periodo.trim().isEmpty()) ? periodo : "MENSUAL";
        Long sucursalFinal = resolverSucursalPorRol(rol, sucursalId, sucursalAutenticada);

        try {
            ReporteCumplimientoDto datosReporte = reporteService.generarReporteDeCumplimiento(
                    kpiFinal, sucursalFinal, periodoFinal, rol, sucursalAutenticada
            );
            byte[] pdfBytes = pdfService.generarPdfDeCumplimiento(datosReporte);
            reporteService.guardarEnHistorial(kpiFinal, sucursalFinal, datosReporte.getNombreKpi(),
                    periodoFinal, datosReporte.getVentasReales(), datosReporte.getEstado(), pdfBytes);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Reporte_Cordillera_" + periodoFinal + ".pdf");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al generar descarga", "details", e.getMessage()));
        }
    }

    // 4. Enviar reporte analítico por correo electrónico
    @Operation(
            summary = "Enviar reporte PDF por correo",
            description = "Genera el reporte de cumplimiento en tiempo real y lo envía como archivo adjunto a la dirección de correo especificada."
    )
    @ApiResponse(responseCode = "200", description = "Correo enviado con éxito")
    @PostMapping("/enviar")
    public ResponseEntity<String> enviarReportePorCorreo(
            @Parameter(description = "ID del KPI") @RequestParam(required = false) Long kpiId,
            @Parameter(description = "ID de la sucursal (solo ADMIN)") @RequestParam(required = false) Long sucursalId,
            @Parameter(description = "Periodo de evaluación") @RequestParam(required = false) String periodo,
            @Parameter(description = "Correo electrónico del destinatario", required = true) @RequestParam String correoDestino,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", required = false) String rol,
            @Parameter(hidden = true) @RequestHeader(value = "X-Sucursal-Id", required = false) Long sucursalAutenticada) {

        Long kpiFinal = (kpiId != null && kpiId != 0) ? kpiId : 1L;
        String periodoFinal = (periodo != null && !periodo.trim().isEmpty()) ? periodo : "MENSUAL";
        Long sucursalFinal = resolverSucursalPorRol(rol, sucursalId, sucursalAutenticada);

        try {
            ReporteCumplimientoDto datosReporte = reporteService.generarReporteDeCumplimiento(
                    kpiFinal, sucursalFinal, periodoFinal, rol, sucursalAutenticada
            );
            byte[] pdfBytes = pdfService.generarPdfDeCumplimiento(datosReporte);
            emailService.enviarReporteConAdjunto(correoDestino, pdfBytes, periodoFinal);
            return ResponseEntity.ok("Reporte enviado exitosamente al correo: " + correoDestino);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al despachar el correo: " + e.getMessage());
        }
    }

    // 5. Previsualización del reporte en navegador (inline PDF)
    @Operation(
            summary = "Previsualizar PDF en el navegador",
            description = "Genera el reporte PDF y lo devuelve con la cabecera 'inline' para que el navegador lo muestre en pantalla en lugar de descargarlo."
    )
    @GetMapping("/previsualizar")
    public ResponseEntity<?> previsualizarReporte(
            @Parameter(description = "ID del KPI") @RequestParam(required = false) Long kpiId,
            @Parameter(description = "ID de la sucursal") @RequestParam(required = false) Long sucursalId,
            @Parameter(description = "Periodo de evaluación") @RequestParam(required = false) String periodo,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", required = false) String rol,
            @Parameter(hidden = true) @RequestHeader(value = "X-Sucursal-Id", required = false) Long sucursalAutenticada) {

        Long kpiFinal = (kpiId != null && kpiId != 0) ? kpiId : 1L;
        String periodoFinal = (periodo != null && !periodo.trim().isEmpty()) ? periodo : "MENSUAL";
        Long sucursalFinal = resolverSucursalPorRol(rol, sucursalId, sucursalAutenticada);

        try {
            ReporteCumplimientoDto datosReporte = reporteService.generarReporteDeCumplimiento(
                    kpiFinal, sucursalFinal, periodoFinal, rol, sucursalAutenticada
            );
            byte[] pdfBytes = pdfService.generarPdfDeCumplimiento(datosReporte);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            // El truco para que se abra en el navegador es 'inline'
            headers.add("Content-Disposition", "inline; filename=Previsualizacion.pdf");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al compilar PDF", "details", e.getMessage()));
        }
    }

    // Helper de resolución multi-tenancy
    private Long resolverSucursalPorRol(
            String rol,
            Long sucursalId,
            Long sucursalAutenticada) {

        if ("ADMIN".equalsIgnoreCase(rol)) {
            return sucursalId;
        }

        return sucursalAutenticada;
    }
}
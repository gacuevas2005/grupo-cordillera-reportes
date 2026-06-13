package com.grupoCordillera.reportes.Controller;

import com.grupoCordillera.reportes.service.ReporteService;
import com.grupoCordillera.reportes.service.PdfService;
import com.grupoCordillera.reportes.service.EmailService;
import com.grupoCordillera.reportes.dto.ReporteCumplimientoDto;
import com.grupoCordillera.reportes.dto.HistorialResumenDto;
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
// ✅ @CrossOrigin eliminado: el CORS lo gestiona exclusivamente el BFF (SecurityConfig.java).
// Tener @CrossOrigin aquí además del BFF duplicaba el header Access-Control-Allow-Origin.
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private EmailService emailService;

    // 1. Obtener historial de reportes con seguridad de sucursal
    @GetMapping("/historial")
    public ResponseEntity<List<HistorialResumenDto>> obtenerHistorial(
            @RequestHeader(value = "X-User-Role", required = false) String rol,
            @RequestHeader(value = "X-Sucursal-Id", required = false) Long sucursalAutenticada) {
        if (rol == null || rol.trim().isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        List<HistorialResumenDto> historial = reporteService.listarHistorialPorSeguridad(rol, sucursalAutenticada);
        return ResponseEntity.ok(historial);
    }

    // 2. Descargar un reporte antiguo desde la base de datos (binario PDF)
    @GetMapping("/historial/{id}/descargar")
    public ResponseEntity<byte[]> descargarReporteAntiguo(@PathVariable Long id) {
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
    @GetMapping("/descargar")
    public ResponseEntity<?> generarYDescargarReporte(
            @RequestParam(required = false) Long kpiId,
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false) String periodo,
            @RequestHeader(value = "X-User-Role", required = false) String rol,
            @RequestHeader(value = "X-Sucursal-Id", required = false) Long sucursalAutenticada) {
        Long kpiFinal = (kpiId != null && kpiId != 0) ? kpiId : 1L;
        String periodoFinal = (periodo != null && !periodo.trim().isEmpty()) ? periodo : "MENSUAL";
        System.out.println("ROL HEADER: " + rol);
        System.out.println("SUCURSAL HEADER: " + sucursalAutenticada);
        System.out.println("SUCURSAL REQUEST PARAM: " + sucursalId);
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
    @PostMapping("/enviar")
    public ResponseEntity<String> enviarReportePorCorreo(
            @RequestParam(required = false) Long kpiId,
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false) String periodo,
            @RequestParam String correoDestino,
            @RequestHeader(value = "X-User-Role", required = false) String rol,
            @RequestHeader(value = "X-Sucursal-Id", required = false) Long sucursalAutenticada) {
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
    @GetMapping("/previsualizar")
    public ResponseEntity<?> previsualizarReporte(
            @RequestParam(required = false) Long kpiId,
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false) String periodo,
            @RequestHeader(value = "X-User-Role", required = false) String rol,
            @RequestHeader(value = "X-Sucursal-Id", required = false) Long sucursalAutenticada) {
        Long kpiFinal = (kpiId != null && kpiId != 0) ? kpiId : 1L;
        String periodoFinal = (periodo != null && !periodo.trim().isEmpty()) ? periodo : "MENSUAL";
        Long sucursalFinal = resolverSucursalPorRol(rol, sucursalId, sucursalAutenticada);
        System.out.println("[MS-REPORTES] -> Previsualizando PDF. KPI: " + kpiFinal + " | Sucursal: " + sucursalFinal);
        try {
            ReporteCumplimientoDto datosReporte = reporteService.generarReporteDeCumplimiento(
                    kpiFinal, sucursalFinal, periodoFinal, rol, sucursalAutenticada
            );
            byte[] pdfBytes = pdfService.generarPdfDeCumplimiento(datosReporte);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.add("Content-Disposition", "inline; filename=Previsualizacion.pdf");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("[ERROR CRÍTICO] -> " + e.getMessage());
            e.printStackTrace();
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
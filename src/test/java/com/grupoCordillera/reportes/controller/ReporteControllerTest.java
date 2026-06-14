package com.grupoCordillera.reportes.controller;

import com.grupoCordillera.reportes.dto.ReporteCumplimientoDto;
import com.grupoCordillera.reportes.service.EmailService;
import com.grupoCordillera.reportes.service.PdfService;
import com.grupoCordillera.reportes.service.ReporteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = com.grupoCordillera.reportes.controller.ReporteController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class}
)
class ReporteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReporteService reporteService;

    @MockBean
    private PdfService pdfService;

    @MockBean
    private EmailService emailService;

    @Test
    void descargarPdfDebeRetornarArchivoYGuardarHistorial() throws Exception {
        // Arrange
        Long kpiId = 1L;
        Long sucursalId = 1L;
        String periodo = "2026-06";
        String rol = "ADMIN"; // Simulamos el rol
        byte[] pdfFalso = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF

        ReporteCumplimientoDto reporteFalso = new ReporteCumplimientoDto();
        reporteFalso.setNombreKpi("Meta Prueba");
        reporteFalso.setVentasReales(100.0);
        reporteFalso.setEstado("OK");

        // 🛠️ CORRECCIÓN 1: Ahora recibe 5 argumentos (incluyendo rol y sucursalAutenticada)
        when(reporteService.generarReporteDeCumplimiento(kpiId, sucursalId, periodo, rol, sucursalId))
                .thenReturn(reporteFalso);
        when(pdfService.generarPdfDeCumplimiento(reporteFalso)).thenReturn(pdfFalso);

        // Act & Assert
        mockMvc.perform(get("/api/reportes/descargar")
                        .param("kpiId", kpiId.toString())
                        .param("sucursalId", sucursalId.toString())
                        .param("periodo", periodo)
                        // 👈 Inyectamos las cabeceras HTTP que espera el nuevo controlador
                        .header("X-User-Role", rol)
                        .header("X-Sucursal-Id", sucursalId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().exists("Content-Disposition"));

        // 🛠️ CORRECCIÓN 2: El historial ahora recibe 7 argumentos (Se añadió sucursalId)
        verify(reporteService, times(1)).guardarEnHistorial(
                eq(kpiId),
                eq(sucursalId), // 👈 Argumento nuevo requerido
                eq("Meta Prueba"),
                eq(periodo),
                eq(100.0),
                eq("OK"),
                eq(pdfFalso)
        );
    }

    @Test
    void enviarPdfPorCorreoDebeRetornarTextoOkYEnviarEmail() throws Exception {
        // Arrange
        Long kpiId = 1L;
        Long sucursalId = 1L;
        String periodo = "2026-06";
        String correo = "admin@cordillera.cl";
        String rol = "ADMIN";
        byte[] pdfFalso = new byte[]{10, 20, 30};

        ReporteCumplimientoDto reporteFalso = new ReporteCumplimientoDto();
        reporteFalso.setNombreKpi("Meta Prueba");
        reporteFalso.setVentasReales(100.0);
        reporteFalso.setEstado("OK");

        // 🛠️ CORRECCIÓN 1: Ahora recibe 5 argumentos
        when(reporteService.generarReporteDeCumplimiento(kpiId, sucursalId, periodo, rol, sucursalId))
                .thenReturn(reporteFalso);
        when(pdfService.generarPdfDeCumplimiento(reporteFalso)).thenReturn(pdfFalso);

        // Act & Assert
        mockMvc.perform(post("/api/reportes/enviar")
                        .param("kpiId", kpiId.toString())
                        .param("sucursalId", sucursalId.toString())
                        .param("periodo", periodo)
                        .param("correoDestino", correo)
                        // 👈 Inyectamos las cabeceras
                        .header("X-User-Role", rol)
                        .header("X-Sucursal-Id", sucursalId.toString()))
                .andExpect(status().isOk())
                // 🛠️ CORRECCIÓN 3: Ajustamos el texto esperado para que coincida exactamente con el Controller
                .andExpect(content().string("Reporte enviado exitosamente al correo: " + correo));

        // 🛠️ CORRECCIÓN 4: Eliminamos el 'verify' de guardarEnHistorial porque el nuevo controlador ya no lo usa aquí.
        verify(emailService, times(1)).enviarReporteConAdjunto(correo, pdfFalso, periodo);
    }
}
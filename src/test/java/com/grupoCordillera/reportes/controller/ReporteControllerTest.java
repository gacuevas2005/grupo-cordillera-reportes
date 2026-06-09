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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ReporteController.class,
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
        byte[] pdfFalso = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF

        ReporteCumplimientoDto reporteFalso = new ReporteCumplimientoDto();
        reporteFalso.setNombreKpi("Meta Prueba");
        reporteFalso.setVentasReales(100.0);
        reporteFalso.setEstado("OK");

        when(reporteService.generarReporteDeCumplimiento(kpiId, sucursalId, periodo)).thenReturn(reporteFalso);
        when(pdfService.generarPdfDeCumplimiento(reporteFalso)).thenReturn(pdfFalso);

        // Act & Assert
        mockMvc.perform(get("/api/reportes/descargar")
                        .param("kpiId", kpiId.toString())
                        .param("sucursalId", sucursalId.toString())
                        .param("periodo", periodo))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().exists("Content-Disposition"));

        // Verificamos que se haya guardado en el historial
        verify(reporteService, times(1)).guardarEnHistorial(eq(kpiId), eq("Meta Prueba"), eq(periodo), eq(100.0), eq("OK"), eq(pdfFalso));
    }

    @Test
    void enviarPdfPorCorreoDebeRetornarTextoOkYEnviarEmail() throws Exception {
        // Arrange
        Long kpiId = 1L;
        Long sucursalId = 1L;
        String periodo = "2026-06";
        String correo = "admin@cordillera.cl";
        byte[] pdfFalso = new byte[]{10, 20, 30};

        ReporteCumplimientoDto reporteFalso = new ReporteCumplimientoDto();
        reporteFalso.setNombreKpi("Meta Prueba");
        reporteFalso.setVentasReales(100.0);
        reporteFalso.setEstado("OK");

        when(reporteService.generarReporteDeCumplimiento(kpiId, sucursalId, periodo)).thenReturn(reporteFalso);
        when(pdfService.generarPdfDeCumplimiento(reporteFalso)).thenReturn(pdfFalso);

        // Act & Assert
        mockMvc.perform(post("/api/reportes/enviar")
                        .param("kpiId", kpiId.toString())
                        .param("sucursalId", sucursalId.toString())
                        .param("periodo", periodo)
                        .param("correoDestino", correo))
                .andExpect(status().isOk())
                .andExpect(content().string("Reporte enviado con éxito a " + correo));

        // Verificamos orquestación
        verify(reporteService, times(1)).guardarEnHistorial(any(), anyString(), anyString(), anyDouble(), anyString(), any());
        verify(emailService, times(1)).enviarReporteConAdjunto(correo, pdfFalso, periodo);
    }
}
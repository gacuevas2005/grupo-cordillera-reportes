package com.grupoCordillera.reportes.service;

import com.grupoCordillera.reportes.dto.ReporteCumplimientoDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class PdfServiceTest {

    @InjectMocks
    private PdfService pdfService;

    @Test
    void debeGenerarArregloDeBytesParaPdfConGraficos() {
        // Arrange (Preparar)
        // 🌟 LA SOLUCIÓN: Creamos un DTO real y lo llenamos de datos.
        ReporteCumplimientoDto reporteReal = new ReporteCumplimientoDto();

        reporteReal.setNombreKpi("Meta de Ventas");
        reporteReal.setMetaEstablecida(1000000.0);
        reporteReal.setVentasReales(800000.0);
        reporteReal.setEstado("Alerta");
        reporteReal.setPorcentajeCumplimiento(80.0);

        // Llenamos las colecciones para evitar errores al dibujar los gráficos
        reporteReal.setTotalesPorSucursal(Map.of("Sucursal Central", 800000.0));
        reporteReal.setDetalleVentas(Collections.emptyList());
        reporteReal.setTopProductos(Map.of("Laptop Ultra", 5));

        // Act (Actuar)
        // Le pasamos el objeto real a tu método
        byte[] resultadoPdf = pdfService.generarPdfDeCumplimiento(reporteReal);

        // Assert (Comprobar)
        assertNotNull(resultadoPdf, "El PDF generado no debe ser nulo");
        assertTrue(resultadoPdf.length > 0, "El arreglo de bytes del PDF debe tener contenido");
    }
}
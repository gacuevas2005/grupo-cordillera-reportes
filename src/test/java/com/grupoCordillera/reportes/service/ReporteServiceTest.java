package com.grupoCordillera.reportes.service;

import com.grupoCordillera.reportes.client.KpiClient;
import com.grupoCordillera.reportes.client.ProductoClient;
import com.grupoCordillera.reportes.client.SucursalClient;
import com.grupoCordillera.reportes.client.VentaClient;
import com.grupoCordillera.reportes.dto.*;
import com.grupoCordillera.reportes.entity.ReporteHistorial;
import com.grupoCordillera.reportes.repository.ReporteHistorialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private VentaClient ventaClient;
    @Mock
    private KpiClient kpiClient;
    @Mock
    private ProductoClient productoClient;
    @Mock
    private SucursalClient sucursalClient;
    @Mock
    private ReporteHistorialRepository historialRepository;

    @InjectMocks
    private ReporteService reporteService;

    @Test
    void debeGenerarReporteDeCumplimientoExitosamente() {
        // --- ARRANGE ---
        Long kpiId = 1L;
        Long sucursalId = 1L;
        String periodo = "MENSUAL";
        String rol = "ADMIN";

        // Mock KPI Definición
        KpiDefinicionDto kpi = new KpiDefinicionDto();
        kpi.setNombre("Meta de Ventas");
        kpi.setValorObjetivo(1000000.0);
        kpi.setTipoCalculo("SUMAR_MONTO");
        when(kpiClient.obtenerDefinicion(kpiId)).thenReturn(kpi);

        // ✅ NUEVO: Mock de las métricas obtenidas directamente del ms-kpi
        KpiMetricaDto metricaReal = new KpiMetricaDto();
        metricaReal.setSucursalId(1L);
        metricaReal.setValorActual(800000.0);
        when(kpiClient.obtenerMetricasPorDefinicion(eq(kpiId), eq(rol), eq(sucursalId)))
                .thenReturn(List.of(metricaReal));

        // Mock de Productos y Sucursales
        ProductoResponseDto producto = new ProductoResponseDto();
        producto.setId(10L);
        producto.setNombre("Laptop Pro");
        when(productoClient.listarTodosLosProductos()).thenReturn(List.of(producto));

        SucursalResponseDto sucursal = new SucursalResponseDto();
        sucursal.setId(1L);
        sucursal.setNombre("Cordillera Central");
        when(sucursalClient.listarTodasLasSucursales()).thenReturn(List.of(sucursal));

        // Mock de Ventas para el Detalle (Gráficos y Tablas)
        VentaResponseDto venta = new VentaResponseDto();
        venta.setProductoId(10L);
        venta.setSucursalId(1L);
        venta.setCantidad(2);
        venta.setMontoTotal(800000.0);
        venta.setFechaVenta(LocalDateTime.now().minusDays(5));
        when(ventaClient.listarTodasLasVentas()).thenReturn(List.of(venta));

        // --- ACT (Ahora incluye los 5 argumentos) ---
        ReporteCumplimientoDto resultado = reporteService.generarReporteDeCumplimiento(kpiId, sucursalId, periodo, rol, sucursalId);

        // --- ASSERT ---
        assertNotNull(resultado);
        assertEquals("Meta de Ventas", resultado.getNombreKpi());
        assertEquals(1000000.0, resultado.getMetaEstablecida());
        assertEquals(800000.0, resultado.getVentasReales()); // Sigue dando 800k gracias al nuevo Mock
        assertEquals(80.0, resultado.getPorcentajeCumplimiento());
        assertEquals("EN RIESGO", resultado.getEstado());

        assertEquals(1, resultado.getDetalleVentas().size());
        assertTrue(resultado.getTotalesPorSucursal().containsKey("Cordillera Central"));
        assertTrue(resultado.getTopProductos().containsKey("Laptop Pro"));
    }

    @Test
    void debeGuardarEnHistorialExitosamente() {
        // Arrange
        Long kpiId = 1L;
        Long sucursalId = 1L; // ✅ Argumento faltante
        String nombre = "Meta Ventas";
        String periodo = "MENSUAL";
        Double ventas = 5000.0;
        String estado = "CRÍTICO";
        byte[] pdf = new byte[]{1, 2, 3};

        // Act (Ahora incluye los 7 argumentos)
        reporteService.guardarEnHistorial(kpiId, sucursalId, nombre, periodo, ventas, estado, pdf);

        // Assert
        verify(historialRepository, times(1)).save(any(ReporteHistorial.class));
    }

    @Test
    void debeListarHistorialMapeadoCorrectamente() {
        // Arrange
        // Creamos un Mock del DTO de Resumen que ahora devuelve el Repositorio
        HistorialResumenDto mockResumen = mock(HistorialResumenDto.class);
        when(mockResumen.getId()).thenReturn(99L);
        when(mockResumen.getNombreKpi()).thenReturn("Ventas Anuales");

        // Simulamos la nueva query del repositorio
        when(historialRepository.obtenerTodosLosResumenes()).thenReturn(List.of(mockResumen));

        // Act (Llamada al método con su nuevo nombre y argumentos de seguridad)
        List<HistorialResumenDto> resultado = reporteService.listarHistorialPorSeguridad("ADMIN", 1L);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(99L, resultado.get(0).getId());
        assertEquals("Ventas Anuales", resultado.get(0).getNombreKpi());
    }

    @Test
    void debeDescargarPdfHistoricoSiExiste() {
        // Arrange
        ReporteHistorial historial = new ReporteHistorial();
        historial.setArchivoPdf(new byte[]{0x25, 0x50, 0x44, 0x46});

        when(historialRepository.findById(5L)).thenReturn(Optional.of(historial));

        // Act
        byte[] pdfRecuperado = reporteService.descargarPdfHistorico(5L);

        // Assert
        assertNotNull(pdfRecuperado);
        assertEquals(4, pdfRecuperado.length);
        verify(historialRepository, times(1)).findById(5L);
    }

    @Test
    void debeLanzarExcepcionSiHistorialNoExiste() {
        // Arrange
        when(historialRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> {
            reporteService.descargarPdfHistorico(999L);
        });

        // ✅ Se actualizó el texto para que coincida con la nueva versión del código real
        assertEquals("Reporte no encontrado con ID: 999", excepcion.getMessage());
    }
}
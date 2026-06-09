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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    // 1. Simulamos todos los clientes externos
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

    // 2. Inyectamos los simulacros en el servicio real
    @InjectMocks
    private ReporteService reporteService;

    @Test
    void debeGenerarReporteDeCumplimientoExitosamente() {
        // --- ARRANGE (Preparar Escenario) ---
        Long kpiId = 1L;
        Long sucursalId = 1L;
        String periodo = "MENSUAL";

        // Simulamos la respuesta del KPI (Meta: 1,000,000)
        KpiDefinicionDto kpi = new KpiDefinicionDto();
        kpi.setNombre("Meta de Ventas");
        kpi.setValorObjetivo(1000000.0);
        when(kpiClient.obtenerDefinicion(kpiId)).thenReturn(kpi);

        // Simulamos la respuesta de Productos
        ProductoResponseDto producto = new ProductoResponseDto();
        producto.setId(10L);
        producto.setNombre("Laptop Pro");
        when(productoClient.listarTodosLosProductos()).thenReturn(List.of(producto));

        // Simulamos la respuesta de Sucursales
        SucursalResponseDto sucursal = new SucursalResponseDto();
        sucursal.setId(1L);
        sucursal.setNombre("Cordillera Central");
        when(sucursalClient.listarTodasLasSucursales()).thenReturn(List.of(sucursal));

        // Simulamos una Venta reciente (para que pase el filtro de fecha)
        VentaResponseDto venta = new VentaResponseDto();
        venta.setProductoId(10L);
        venta.setSucursalId(1L);
        venta.setCantidad(2);
        venta.setMontoTotal(800000.0);
        venta.setFechaVenta(LocalDateTime.now().minusDays(5)); // Hace 5 días, entra en MENSUAL
        when(ventaClient.listarTodasLasVentas()).thenReturn(List.of(venta));

        // --- ACT (Actuar) ---
        ReporteCumplimientoDto resultado = reporteService.generarReporteDeCumplimiento(kpiId, sucursalId, periodo);

        // --- ASSERT (Comprobar) ---
        assertNotNull(resultado);
        assertEquals("Meta de Ventas", resultado.getNombreKpi());
        assertEquals(1000000.0, resultado.getMetaEstablecida());
        assertEquals(800000.0, resultado.getVentasReales());

        // Matemáticas: (800k / 1M) * 100 = 80.0% -> Debería ser estado "EN RIESGO" según tu lógica
        assertEquals(80.0, resultado.getPorcentajeCumplimiento());
        assertEquals("EN RIESGO", resultado.getEstado());

        // Verificamos que los agrupamientos funcionen
        assertEquals(1, resultado.getDetalleVentas().size());
        assertTrue(resultado.getTotalesPorSucursal().containsKey("Cordillera Central"));
        assertTrue(resultado.getTopProductos().containsKey("Laptop Pro"));
    }

    @Test
    void debeGuardarEnHistorialExitosamente() {
        // Arrange
        Long kpiId = 1L;
        String nombre = "Meta Ventas";
        String periodo = "MENSUAL";
        Double ventas = 5000.0;
        String estado = "CRÍTICO";
        byte[] pdf = new byte[]{1, 2, 3};

        // Act
        reporteService.guardarEnHistorial(kpiId, nombre, periodo, ventas, estado, pdf);

        // Assert
        // Verificamos que el repositorio intentó guardar una entidad exactamente 1 vez
        verify(historialRepository, times(1)).save(any(ReporteHistorial.class));
    }

    @Test
    void debeListarHistorialMapeadoCorrectamente() {
        // Arrange
        ReporteHistorial historialReal = new ReporteHistorial();
        historialReal.setId(99L);
        historialReal.setNombreKpi("Ventas Anuales");
        historialReal.setVentasReales(10000.0);

        when(historialRepository.findAll()).thenReturn(List.of(historialReal));

        // Act
        List<HistorialResumenDto> resultado = reporteService.listarHistorial();

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
        historial.setArchivoPdf(new byte[]{0x25, 0x50, 0x44, 0x46}); // Simula un PDF

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

        assertEquals("Reporte histórico no encontrado", excepcion.getMessage());
    }
}
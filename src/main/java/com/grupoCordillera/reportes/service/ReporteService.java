package com.grupoCordillera.reportes.service;

import com.grupoCordillera.reportes.client.KpiClient;
import com.grupoCordillera.reportes.client.ProductoClient;
import com.grupoCordillera.reportes.client.SucursalClient;
import com.grupoCordillera.reportes.client.VentaClient;
import com.grupoCordillera.reportes.dto.*;
import com.grupoCordillera.reportes.entity.ReporteHistorial;
import com.grupoCordillera.reportes.repository.ReporteHistorialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReporteService {

    @Autowired
    private VentaClient ventaClient;

    @Autowired
    private KpiClient kpiClient;

    @Autowired
    private ProductoClient productoClient;

    @Autowired
    private SucursalClient sucursalClient;

    @Autowired
    private ReporteHistorialRepository historialRepository;

    public ReporteCumplimientoDto generarReporteDeCumplimiento(Long kpiId, Long sucursalId, String periodo) {

        KpiDefinicionDto kpiActual = kpiClient.obtenerDefinicion(kpiId);
        Double meta = kpiActual.getValorObjetivo();

        Map<Long, String> mapaProductos = productoClient.listarTodosLosProductos().stream()
                .collect(Collectors.toMap(ProductoResponseDto::getId, ProductoResponseDto::getNombre));

        Map<Long, String> mapaSucursales = sucursalClient.listarTodasLasSucursales().stream()
                .collect(Collectors.toMap(SucursalResponseDto::getId, SucursalResponseDto::getNombre));


        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fechaLimite;

        switch (periodo.toUpperCase()) {
            case "SEMANAL":
                fechaLimite = ahora.minusWeeks(1); // Hace 7 días
                break;
            case "ANUAL":
                fechaLimite = ahora.minusYears(1); // Hace 365 días
                break;
            case "MENSUAL":
            default:
                fechaLimite = ahora.minusMonths(1); // Hace 1 mes
                break;
        }

        List<VentaResponseDto> todasLasVentas = ventaClient.listarTodasLasVentas();

        // --- 2. EL FILTRO DE TIEMPO Y SUCURSAL ---
        List<VentaDetalleDto> detalleVentas = todasLasVentas.stream()
                // Descartar ventas sin fecha o que ocurrieron ANTES de la fecha límite
                .filter(v -> v.getFechaVenta() != null && v.getFechaVenta().isAfter(fechaLimite))
                .map(v -> {
                    VentaDetalleDto dto = new VentaDetalleDto();
                    dto.setProductoId(v.getProductoId());
                    dto.setProductoNombre(mapaProductos.getOrDefault(v.getProductoId(), "Desconocido"));
                    dto.setSucursalNombre(mapaSucursales.getOrDefault(v.getSucursalId(), "Desconocida"));
                    dto.setCantidad(v.getCantidad());
                    dto.setMontoTotal(v.getMontoTotal());
                    return dto;
                }).collect(Collectors.toList());

        // 4. Calcular Total por Sucursal usando Streams (¡Puro poder de Java!)
        Map<String, Double> totalesPorSucursal = detalleVentas.stream()
                .collect(Collectors.groupingBy(
                        VentaDetalleDto::getSucursalNombre,
                        Collectors.summingDouble(VentaDetalleDto::getMontoTotal)
                ));

        // 5. Calcular totales generales para la sucursal específica
        Double ventasReales = detalleVentas.stream()
                .mapToDouble(VentaDetalleDto::getMontoTotal)
                .sum();

        Double porcentaje = (ventasReales / meta) * 100;
        String estado = (porcentaje >= 100) ? "SUPERADO" : (porcentaje >= 80) ? "EN RIESGO" : "CRÍTICO";

        Map<String, Integer> topProductos = detalleVentas.stream()
                // 1. Agrupamos por nombre de producto y sumamos las cantidades
                .collect(Collectors.groupingBy(
                        VentaDetalleDto::getProductoNombre,
                        Collectors.summingInt(VentaDetalleDto::getCantidad)
                ))
                .entrySet().stream()
                // 2. Ordenamos de mayor a menor (reversed)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                // 3. Tomamos solo los 10 primeros
                .limit(10)
                // 4. Lo volvemos a convertir en un Mapa, usando LinkedHashMap para NO perder el orden
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        // 6. Armar el informe final
        ReporteCumplimientoDto reporte = new ReporteCumplimientoDto();
        reporte.setNombreKpi(kpiActual.getNombre());
        reporte.setMetaEstablecida(meta);
        reporte.setVentasReales(ventasReales);
        reporte.setPorcentajeCumplimiento(Math.round(porcentaje * 100.0) / 100.0);
        reporte.setEstado(estado);
        reporte.setDetalleVentas(detalleVentas);
        reporte.setTotalesPorSucursal(totalesPorSucursal);
        reporte.setTopProductos(topProductos);

        return reporte;
    }

    public void guardarEnHistorial(Long kpiId, String nombreKpi, String periodo, Double ventasReales, String estadoFinal, byte[] pdfBytes) {
        ReporteHistorial historial = new ReporteHistorial();
        historial.setKpiId(kpiId);
        historial.setNombreKpi(nombreKpi);
        historial.setPeriodo(periodo);
        historial.setVentasReales(ventasReales);
        historial.setEstadoFinal(estadoFinal);
        historial.setArchivoPdf(pdfBytes);

        historialRepository.save(historial);
    }
    public List<HistorialResumenDto> listarHistorial() {
        return historialRepository.findAll().stream().map(h -> {
            HistorialResumenDto dto = new HistorialResumenDto();
            dto.setId(h.getId());
            dto.setNombreKpi(h.getNombreKpi());
            dto.setPeriodo(h.getPeriodo());
            dto.setVentasReales(h.getVentasReales());
            dto.setEstadoFinal(h.getEstadoFinal());
            dto.setFechaGeneracion(h.getFechaGeneracion());
            return dto;
        }).collect(Collectors.toList());
    }
    public byte[] descargarPdfHistorico(Long id) {
        ReporteHistorial historial = historialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reporte histórico no encontrado"));

        return historial.getArchivoPdf(); // Devolvemos el archivo binario
    }
}
package com.grupoCordillera.reportes.service;

import com.grupoCordillera.reportes.client.KpiClient;
import com.grupoCordillera.reportes.client.ProductoClient;
import com.grupoCordillera.reportes.client.SucursalClient;
import com.grupoCordillera.reportes.client.VentaClient;
import com.grupoCordillera.reportes.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

        // 6. Armar el informe final
        ReporteCumplimientoDto reporte = new ReporteCumplimientoDto();
        reporte.setNombreKpi(kpiActual.getNombre());
        reporte.setMetaEstablecida(meta);
        reporte.setVentasReales(ventasReales);
        reporte.setPorcentajeCumplimiento(Math.round(porcentaje * 100.0) / 100.0);
        reporte.setEstado(estado);
        reporte.setDetalleVentas(detalleVentas);
        reporte.setTotalesPorSucursal(totalesPorSucursal);

        return reporte;
    }
}
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

import java.time.LocalDateTime;
import java.util.*;
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

    public ReporteCumplimientoDto generarReporteDeCumplimiento(Long kpiId, Long sucursalId, String periodo, String userRole, Long sucursalAutenticada) {

        // 1. AISLAMIENTO MULTI-TENANCY: Si el solicitante es GERENTE, se fuerza su sucursal del token JWT
        if (userRole != null && !"ADMIN".equalsIgnoreCase(userRole.trim())) {
            System.out.println("[SEGURIDAD] -> Rol restringido: Forzando Sucursal ID " + sucursalAutenticada);
            sucursalId = sucursalAutenticada;
        }
        final Long sucursalFiltroFinal = sucursalId;

        // 2. CONSUMO DEL KPICLIENT: Metadata de la definición (nombre, meta, tipoCalculo)
        String nombreKpiDetectado = "Métrica de Rendimiento Comercial";
        String tipoCalculo = "SUMAR_MONTO";
        Double meta = 0.0;

        try {
            KpiDefinicionDto definicionKpi = kpiClient.obtenerDefinicion(kpiId);
            if (definicionKpi != null) {
                if (definicionKpi.getValorObjetivo() != null) meta = definicionKpi.getValorObjetivo();
                if (definicionKpi.getNombre() != null) nombreKpiDetectado = definicionKpi.getNombre();
                if (definicionKpi.getTipoCalculo() != null) tipoCalculo = definicionKpi.getTipoCalculo();
            }
        } catch (Exception e) {
            System.out.println("[ADVERTENCIA] -> No se pudo conectar con ms-kpi para la definición. Usando fallback.");
        }

        // 3. ✅ NUEVO: LEER EL VALOR ACUMULADO REAL DESDE LAS MÉTRICAS DEL KPI
        // El ms-kpi acumula el progreso en kpi_metricas cada vez que ocurre una venta.
        // Ese valorActual es la fuente de verdad: evitamos recalcular desde ventas para este dato.
        Double ventasRealesDesdeKpi = null;
        try {
            List<KpiMetricaDto> metricas = kpiClient.obtenerMetricasPorDefinicion(kpiId, userRole, sucursalFiltroFinal);
            if (metricas != null && !metricas.isEmpty()) {
                if (sucursalFiltroFinal != null) {
                    // Para GERENTE: buscamos la métrica exacta de su sucursal
                    ventasRealesDesdeKpi = metricas.stream()
                            .filter(m -> sucursalFiltroFinal.equals(m.getSucursalId()))
                            .mapToDouble(m -> m.getValorActual() != null ? m.getValorActual() : 0.0)
                            .sum();
                } else {
                    // Para ADMIN: sumamos el valorActual de todas las sucursales
                    ventasRealesDesdeKpi = metricas.stream()
                            .mapToDouble(m -> m.getValorActual() != null ? m.getValorActual() : 0.0)
                            .sum();
                }
                System.out.println("[KPI] -> Valor acumulado leído desde ms-kpi: " + ventasRealesDesdeKpi);
            }
        } catch (Exception e) {
            System.out.println("[ADVERTENCIA] -> No se pudo leer métricas desde ms-kpi. Se calculará desde ventas como fallback.");
        }

        // 4. Diccionarios maestros de productos y sucursales
        Map<Long, String> mapaProductos = Map.of();
        try {
            mapaProductos = productoClient.listarTodosLosProductos().stream()
                    .collect(Collectors.toMap(ProductoResponseDto::getId, ProductoResponseDto::getNombre, (p1, p2) -> p1));
        } catch (Exception e) {
            System.out.println("[ERROR FEIGN] -> No se pudo conectar con ms-productos.");
        }

        Map<Long, String> mapaSucursales = Map.of();
        try {
            mapaSucursales = sucursalClient.listarTodasLasSucursales().stream()
                    .collect(Collectors.toMap(SucursalResponseDto::getId, SucursalResponseDto::getNombre, (s1, s2) -> s1));
        } catch (Exception e) {
            System.out.println("[ERROR FEIGN] -> No se pudo conectar con ms-sucursales.");
        }

        // 5. Filtrado de ventas (usado para el detalle, top productos y gráfico)
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fechaLimite;
        switch (periodo.toUpperCase()) {
            case "SEMANAL": fechaLimite = ahora.minusWeeks(1); break;
            case "ANUAL":   fechaLimite = ahora.minusYears(1); break;
            case "MENSUAL":
            default:        fechaLimite = coderCalcularFechaLimiteMensualOFallback(todasLasVentasDisponibles()); break;
        }

        List<VentaResponseDto> todasLasVentas = new ArrayList<>();
        try {
            todasLasVentas = ventaClient.listarTodasLasVentas();
        } catch (Exception e) {
            System.out.println("[ERROR CRÍTICO FEIGN] -> No se pueden recuperar las ventas de ms-ventas.");
        }

        final Map<Long, String> prodMap = mapaProductos;
        final Map<Long, String> sucMap = mapaSucursales;
        final LocalDateTime limite = fechaLimite;

        List<VentaResponseDto> ventasFiltradas = todasLasVentas.stream()
                .filter(v -> sucursalFiltroFinal == null || v.getSucursalId().equals(sucursalFiltroFinal))
                .collect(Collectors.toList());

        List<VentaDetalleDto> detalleVentas = ventasFiltradas.stream()
                .filter(v -> v.getFechaVenta() == null || v.getFechaVenta().isAfter(limite))
                .map(v -> {
                    VentaDetalleDto dto = new VentaDetalleDto();
                    dto.setProductoId(v.getProductoId());
                    dto.setProductoNombre(prodMap.getOrDefault(v.getProductoId(), "Producto #" + v.getProductoId()));
                    dto.setSucursalNombre(sucMap.getOrDefault(v.getSucursalId(), "Sucursal #" + v.getSucursalId()));
                    dto.setCantidad(v.getCantidad() != null ? v.getCantidad() : 1);
                    dto.setMontoTotal(v.getMontoTotal() != null ? v.getMontoTotal() : 0.0);
                    return dto;
                }).collect(Collectors.toList());

        // 6. Gráfico: totales por sucursal (siempre desde ventas para mostrar distribución)
        Map<String, Double> totalesPorSucursal = detalleVentas.stream()
                .collect(Collectors.groupingBy(
                        VentaDetalleDto::getSucursalNombre,
                        Collectors.summingDouble(VentaDetalleDto::getMontoTotal)
                ));

        if (totalesPorSucursal.size() <= 1) {
            String nombreSucursalActual = sucMap.getOrDefault(sucursalFiltroFinal, "Sucursal " + sucursalFiltroFinal);
            if (totalesPorSucursal.isEmpty()) totalesPorSucursal.put(nombreSucursalActual, 0.0);
            totalesPorSucursal.put("Resto del Holding (Ref)", 0.0);
        }

        // 7. ✅ CÁLCULO FINAL DEL AVANCE REAL
        // Prioridad: valorActual acumulado en ms-kpi (fuente de verdad).
        // Fallback: si ms-kpi no respondió, calculamos desde las ventas locales (comportamiento anterior).
        Double ventasReales;
        if (ventasRealesDesdeKpi != null) {
            // Fuente principal: el valor acumulado que gestiona ms-kpi
            ventasReales = ventasRealesDesdeKpi;
            System.out.println("[REPORTE] -> Usando valorActual de ms-kpi: " + ventasReales);
        } else {
            // Fallback: recalcular desde ventas (igual que antes, por si ms-kpi no está disponible)
            System.out.println("[REPORTE] -> Fallback: calculando ventasReales desde ms-ventas.");
            if ("SUMAR_PRODUCTOS".equalsIgnoreCase(tipoCalculo)) {
                ventasReales = detalleVentas.stream().mapToDouble(VentaDetalleDto::getCantidad).sum();
            } else if ("CONTAR_TRANSACCIONES".equalsIgnoreCase(tipoCalculo)) {
                ventasReales = (double) detalleVentas.size();
            } else {
                ventasReales = detalleVentas.stream().mapToDouble(VentaDetalleDto::getMontoTotal).sum();
            }
        }

        Double porcentaje = (meta > 0) ? (ventasReales / meta) * 100 : 0.0;
        String estado = (porcentaje >= 100) ? "SUPERADO" : (porcentaje >= 80) ? "EN RIESGO" : "CRÍTICO";

        // 8. Top 10 productos por cantidad (siempre desde ventas para el detalle)
        Map<String, Integer> topProductos = detalleVentas.stream()
                .collect(Collectors.groupingBy(
                        VentaDetalleDto::getProductoNombre,
                        Collectors.summingInt(VentaDetalleDto::getCantidad)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        if (topProductos.isEmpty()) {
            topProductos.put("Sin Operaciones", 0);
        }

        // 9. Construcción del DTO de respuesta final
        ReporteCumplimientoDto reporte = new ReporteCumplimientoDto();
        reporte.setNombreSucursal(sucMap.getOrDefault(sucursalFiltroFinal, "Sucursal " + sucursalFiltroFinal));
        reporte.setNombreKpi(nombreKpiDetectado);
        reporte.setMetaEstablecida(meta);
        reporte.setVentasReales(ventasReales);
        reporte.setPorcentajeCumplimiento(Math.round(porcentaje * 100.0) / 100.0);
        reporte.setEstado(estado);
        reporte.setDetalleVentas(detalleVentas);
        reporte.setTotalesPorSucursal(totalesPorSucursal);
        reporte.setTopProductos(topProductos);

        return reporte;
    }

    public void guardarEnHistorial(Long kpiId, Long sucursalId, String nombreKpi, String periodo, Double ventasReales, String estadoFinal, byte[] pdfBytes) {
        System.out.println("GUARDANDO REPORTE EN HISTORIAL");
        System.out.println("SUCURSAL A GUARDAR: " + sucursalId);
        ReporteHistorial historial = new ReporteHistorial();
        historial.setKpiId(kpiId);
        historial.setSucursalId(sucursalId);
        historial.setNombreKpi(nombreKpi);
        historial.setPeriodo(periodo);
        historial.setVentasReales(ventasReales);
        historial.setEstadoFinal(estadoFinal);
        historial.setArchivoPdf(pdfBytes);
        historialRepository.save(historial);

    }

    public List<HistorialResumenDto> listarHistorialPorSeguridad(
            String rol,
            Long sucursalAutenticada) {

        if ("ADMIN".equalsIgnoreCase(rol.trim())) {
            return historialRepository.obtenerTodosLosResumenes();
        }

        return historialRepository.obtenerResumenPorSucursal(sucursalAutenticada);
    }

    public byte[] descargarPdfHistorico(Long id) {

        return historialRepository.findById(id)
                .map(ReporteHistorial::getArchivoPdf)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado con ID: " + id));
    }

    private LocalDateTime coderCalcularFechaLimiteMensualOFallback(List<VentaResponseDto> ventas) {
        if (ventas == null || ventas.isEmpty()) {
            return LocalDateTime.now().minusMonths(1);
        }
        return LocalDateTime.now().minusYears(5);
    }

    private List<VentaResponseDto> todasLasVentasDisponibles() {
        try { return ventaClient.listarTodasLasVentas(); } catch (Exception e) { return new ArrayList<>(); }
    }
}
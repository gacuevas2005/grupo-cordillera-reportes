package com.grupoCordillera.reportes.service;

import com.grupoCordillera.reportes.client.KpiClient;
import com.grupoCordillera.reportes.client.VentaClient;
import com.grupoCordillera.reportes.dto.KpiDefinicionDto;
import com.grupoCordillera.reportes.dto.KpiMetricaDto;
import com.grupoCordillera.reportes.dto.ReporteCumplimientoDto;
import com.grupoCordillera.reportes.dto.VentaResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReporteService {

    @Autowired
    private VentaClient ventaClient;

    @Autowired
    private KpiClient kpiClient;

    public ReporteCumplimientoDto generarReporteDeCumplimiento(Long kpiId, Long sucursalId) {

        // 1. Obtener la meta directamente (sin listas)
        KpiDefinicionDto kpiActual = kpiClient.obtenerDefinicion(kpiId);

        if (kpiActual == null || kpiActual.getValorObjetivo() == null) {
            throw new RuntimeException("KPI no encontrado o sin meta definida");
        }

        Double meta = kpiActual.getValorObjetivo(); // Los 5.000.000

        // 2. Obtener las ventas y filtrar por sucursal
        List<VentaResponseDto> todasLasVentas = ventaClient.listarTodasLasVentas();

        Double ventasReales = todasLasVentas.stream()
                .filter(v -> v.getSucursalId().equals(sucursalId))
                .mapToDouble(VentaResponseDto::getMontoTotal)
                .sum();

        // 3. Calcular el cumplimiento
        Double porcentaje = (ventasReales / meta) * 100;

        String estado;
        if (porcentaje >= 100) {
            estado = "SUPERADO";
        } else if (porcentaje >= 80) {
            estado = "EN RIESGO";
        } else {
            estado = "CRÍTICO";
        }

        // 4. Armar el informe
        ReporteCumplimientoDto reporte = new ReporteCumplimientoDto();
        reporte.setNombreKpi(kpiActual.getNombre());
        reporte.setMetaEstablecida(meta);
        reporte.setVentasReales(ventasReales);
        reporte.setPorcentajeCumplimiento(Math.round(porcentaje * 100.0) / 100.0);
        reporte.setEstado(estado);

        return reporte;
    }
}
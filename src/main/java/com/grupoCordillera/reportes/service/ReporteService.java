package com.grupoCordillera.reportes.service;

import com.grupoCordillera.reportes.client.KpiClient;
import com.grupoCordillera.reportes.client.VentaClient;
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
        // 1. Obtener los datos del KPI (La Meta)
        List<KpiMetricaDto> metricas = kpiClient.listarMetricas(kpiId);

        // Asumimos que tomamos la primera métrica para el ejemplo
        KpiMetricaDto kpiActual = metricas.stream().findFirst()
                .orElseThrow(() -> new RuntimeException("KPI no encontrado"));

        Double meta = kpiActual.getDefinicion().getValorObjetivo(); // Ej: 1,000,000 CLP

        // 2. Obtener las ventas y filtrar por la sucursal que nos interesa
        List<VentaResponseDto> todasLasVentas = ventaClient.listarTodasLasVentas();

        Double ventasReales = todasLasVentas.stream()
                .filter(v -> v.getSucursalId().equals(sucursalId))
                .mapToDouble(VentaResponseDto::getMontoTotal)
                .sum();

        // 3. Calcular el cumplimiento
        Double porcentaje = (ventasReales / meta) * 100;

        // 4. Determinar el estado para el semáforo del Dashboard
        String estado;
        if (porcentaje >= 100) {
            estado = "SUPERADO";
        } else if (porcentaje >= 80) {
            estado = "EN RIESGO";
        } else {
            estado = "CRÍTICO";
        }

        // 5. Armar y retornar el informe
        ReporteCumplimientoDto reporte = new ReporteCumplimientoDto();
        reporte.setNombreKpi(kpiActual.getDefinicion().getNombre());
        reporte.setMetaEstablecida(meta);
        reporte.setVentasReales(ventasReales);
        reporte.setPorcentajeCumplimiento(Math.round(porcentaje * 100.0) / 100.0); // Redondear a 2 decimales
        reporte.setEstado(estado);

        return reporte;
    }
}
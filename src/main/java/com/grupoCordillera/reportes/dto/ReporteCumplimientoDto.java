package com.grupoCordillera.reportes.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ReporteCumplimientoDto {
    private String nombreSucursal;
    private String nombreKpi;
    private Double metaEstablecida;  // Lo que viene de ms-kpi
    private Double ventasReales;     // Lo que calculamos de ms-ventas
    private Double porcentajeCumplimiento;
    private String estado;           // Ej: "SUPERADO", "EN RIESGO", "CRÍTICO"
    private List<VentaDetalleDto> detalleVentas;
    private Map<String, Double> totalesPorSucursal;
}
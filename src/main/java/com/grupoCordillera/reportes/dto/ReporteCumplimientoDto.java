package com.grupoCordillera.reportes.dto;

import lombok.Data;

@Data
public class ReporteCumplimientoDto {
    private String nombreSucursal;
    private String nombreKpi;
    private Double metaEstablecida;  // Lo que viene de ms-kpi
    private Double ventasReales;     // Lo que calculamos de ms-ventas
    private Double porcentajeCumplimiento;
    private String estado;           // Ej: "SUPERADO", "EN RIESGO", "CRÍTICO"
}
package com.grupoCordillera.reportes.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ReporteCumplimientoDto {
    private String nombreSucursal;
    private String nombreKpi;
    private Double metaEstablecida;
    private Double ventasReales;
    private Double porcentajeCumplimiento;
    private String estado;
    private List<VentaDetalleDto> detalleVentas;
    private Map<String, Double> totalesPorSucursal;
    private Map<String, Integer> topProductos;
}
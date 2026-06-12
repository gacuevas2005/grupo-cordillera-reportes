package com.grupoCordillera.reportes.dto;

import lombok.Data;

@Data
public class KpiDefinicionDto {
    private String nombre;
    private Double valorObjetivo; // Esta es la meta que usaremos para el cálculo

    // 🌟 ¡Faltaba este campo para que ReporteService sepa qué tipo de KPI es!
    private String tipoCalculo;
}
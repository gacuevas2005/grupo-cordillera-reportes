package com.grupoCordillera.reportes.dto;

import lombok.Data;

@Data
public class KpiMetricaDto {
    private Long id;
    private Double valorActual;

    // Aquí anidamos la definición que creamos arriba
    private KpiDefinicionDto definicion;
}

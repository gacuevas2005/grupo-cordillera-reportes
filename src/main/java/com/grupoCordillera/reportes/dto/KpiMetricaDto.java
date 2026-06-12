package com.grupoCordillera.reportes.dto;

import lombok.Data;

@Data
public class KpiMetricaDto {
    private Long id;

    // 🌟 ¡Faltaba este campo crítico para identificar la sucursal!
    private Long sucursalId;

    private Double valorActual;

    // Aquí anidamos la definición que creamos arriba
    private KpiDefinicionDto definicion;
}

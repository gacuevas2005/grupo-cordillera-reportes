package com.grupoCordillera.reportes.dto;

import lombok.Data;

@Data
public class KpiDefinicionDto {
    private String nombre;
    private Double valorObjetivo; // Esta es la meta que usaremos para el cálculo
}
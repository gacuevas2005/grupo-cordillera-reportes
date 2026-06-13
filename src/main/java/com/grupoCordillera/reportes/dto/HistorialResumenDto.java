package com.grupoCordillera.reportes.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialResumenDto {
    private Long id;
    private String nombreKpi;
    private String periodo;
    private Double ventasReales;
    private String estadoFinal;
    private LocalDateTime fechaGeneracion;
}
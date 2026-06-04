package com.grupoCordillera.reportes.dto;


import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HistorialResumenDto {
    private Long id;
    private String nombreKpi;
    private String periodo;
    private Double ventasReales;
    private String estadoFinal;
    private LocalDateTime fechaGeneracion;
}
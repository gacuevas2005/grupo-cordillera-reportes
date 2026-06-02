package com.grupoCordillera.reportes.dto;


import lombok.Data;

@Data
public class VentaResponseDto {
    // Solo ponemos los campos que Reportes necesita leer del JSON
    private Long id;
    private Long sucursalId;
    private Double montoTotal;
}
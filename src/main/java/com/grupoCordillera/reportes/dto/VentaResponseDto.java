package com.grupoCordillera.reportes.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VentaResponseDto {
    // Solo ponemos los campos que Reportes necesita leer del JSON
    private Long id;
    private Long sucursalId;
    private Double montoTotal;
    private Long productoId;
    private Integer cantidad;
    private LocalDateTime fechaVenta;
}
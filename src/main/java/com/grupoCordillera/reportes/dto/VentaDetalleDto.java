package com.grupoCordillera.reportes.dto;

import lombok.Data;

@Data
public class VentaDetalleDto {
    private Long productoId;
    private String productoNombre;
    private String sucursalNombre;
    private Integer cantidad;
    private Double montoTotal;
}
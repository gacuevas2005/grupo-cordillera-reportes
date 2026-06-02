package com.grupoCordillera.reportes.client;

import com.grupoCordillera.reportes.dto.VentaResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "ms-ventas", url = "http://localhost:8081")
public interface VentaClient {
    // Reutilizamos el endpoint que ya tienes en Ventas
    @GetMapping("/api/ventas")
    List<VentaResponseDto> listarTodasLasVentas();
}
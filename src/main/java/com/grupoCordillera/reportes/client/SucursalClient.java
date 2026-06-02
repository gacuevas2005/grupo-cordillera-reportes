package com.grupoCordillera.reportes.client;


import com.grupoCordillera.reportes.dto.SucursalResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "ms-sucursales", url = "http://localhost:8084")
public interface SucursalClient {
    @GetMapping("/api/sucursales")
    List<SucursalResponseDto> listarTodasLasSucursales();
}
package com.grupoCordillera.reportes.client;

import com.grupoCordillera.reportes.dto.ProductoResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "ms-productos", url = "http://localhost:8082")
public interface ProductoClient {
    @GetMapping("/api/productos")
    List<ProductoResponseDto> listarTodosLosProductos();
}
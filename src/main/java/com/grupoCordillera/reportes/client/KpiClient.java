package com.grupoCordillera.reportes.client;

import com.grupoCordillera.reportes.dto.KpiDefinicionDto;
import com.grupoCordillera.reportes.dto.KpiMetricaDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "ms-kpi", url = "http://localhost:8087/api/kpi")
public interface KpiClient {

    // Trae la definición del KPI (nombre, meta, tipoCalculo)
    @GetMapping("/definiciones/{id}")
    KpiDefinicionDto obtenerDefinicion(@PathVariable("id") Long id);

    // ✅ NUEVO: Trae las métricas acumuladas reales por definición de KPI
    // Incluye el valorActual por sucursal, que es la fuente de verdad del progreso
    @GetMapping("/metricas/{id}")
    List<KpiMetricaDto> obtenerMetricasPorDefinicion(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-Sucursal-Id", required = false) Long sucursalId
    );
}
package com.grupoCordillera.reportes.client;

import com.grupoCordillera.reportes.dto.KpiDefinicionDto;
import com.grupoCordillera.reportes.dto.KpiMetricaDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "ms-kpi", url = "http://localhost:8087")
public interface KpiClient {
    // Usamos el endpoint que armaste antes
    @GetMapping("/api/kpi/definiciones/{id}")
    KpiDefinicionDto obtenerDefinicion(@PathVariable Long id);
}
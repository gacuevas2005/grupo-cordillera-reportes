package com.grupoCordillera.reportes.client;

import com.grupoCordillera.reportes.dto.KpiDefinicionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// 🎯 SOLUCIONADO: Añadimos la ruta base estructural '/api/kpi' directamente en la URL del Feign
@FeignClient(name = "ms-kpi", url = "http://localhost:8087/api/kpi")
public interface KpiClient {

    // 🎯 CORREGIDO: Dejamos el mapeo limpio desde la raíz del recurso '/definiciones'
    @GetMapping("/definiciones/{id}")
    KpiDefinicionDto obtenerDefinicion(@PathVariable("id") Long id);
}
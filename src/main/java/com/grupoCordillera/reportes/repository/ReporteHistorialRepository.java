package com.grupoCordillera.reportes.repository;


import com.grupoCordillera.reportes.entity.ReporteHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporteHistorialRepository extends JpaRepository<ReporteHistorial, Long> {
    List<ReporteHistorial> findBySucursalId(Long sucursalId);
}
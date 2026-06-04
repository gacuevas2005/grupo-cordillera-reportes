package com.grupoCordillera.reportes.repository;


import com.grupoCordillera.reportes.entity.ReporteHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReporteHistorialRepository extends JpaRepository<ReporteHistorial, Long> {
}
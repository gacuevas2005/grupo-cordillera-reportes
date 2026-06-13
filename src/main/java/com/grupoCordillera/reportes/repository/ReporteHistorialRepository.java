package com.grupoCordillera.reportes.repository;

import com.grupoCordillera.reportes.dto.HistorialResumenDto;
import com.grupoCordillera.reportes.entity.ReporteHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ReporteHistorialRepository extends JpaRepository<ReporteHistorial, Long> {

    @Query("""
        SELECT new com.grupoCordillera.reportes.dto.HistorialResumenDto(
            h.id,
            h.nombreKpi,
            h.periodo,
            h.ventasReales,
            h.estadoFinal,
            h.fechaGeneracion
        )
        FROM ReporteHistorial h
    """)
    List<HistorialResumenDto> obtenerTodosLosResumenes();

    @Query("""
        SELECT new com.grupoCordillera.reportes.dto.HistorialResumenDto(
            h.id,
            h.nombreKpi,
            h.periodo,
            h.ventasReales,
            h.estadoFinal,
            h.fechaGeneracion
        )
        FROM ReporteHistorial h
        WHERE h.sucursalId = :sucursalId
    """)
    List<HistorialResumenDto> obtenerResumenPorSucursal(
            @Param("sucursalId") Long sucursalId
    );
}
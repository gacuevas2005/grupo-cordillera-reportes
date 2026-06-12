package com.grupoCordillera.reportes.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_reportes")
@Data
public class ReporteHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long kpiId;

    // 🔒 MULTI-TENANCY: Campo crítico para segmentar qué sucursal emitió el PDF
    @Column(name = "sucursal_id")
    private Long sucursalId;

    private String nombreKpi;
    private String periodo;
    private Double ventasReales;
    private String estadoFinal;

    @CreationTimestamp
    @Column(name = "fecha_generacion", updatable = false)
    private LocalDateTime fechaGeneracion;

    // 📄 Guardado de los bytes del PDF de forma segura
    @Lob
    @Column(name = "archivo_pdf") // O "BYTEA" si estás usando PostgreSQL
    private byte[] archivoPdf;
}
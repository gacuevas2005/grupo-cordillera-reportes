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
    private String nombreKpi;
    private String periodo;
    private Double ventasReales;
    private String estadoFinal;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fechaGeneracion;

    // Aquí guardaremos los bytes del PDF
    @Lob
    private byte[] archivoPdf;
}
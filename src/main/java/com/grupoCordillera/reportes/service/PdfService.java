package com.grupoCordillera.reportes.service;


import com.grupoCordillera.reportes.dto.ReporteCumplimientoDto;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfService {

    public byte[] generarPdfDeCumplimiento(ReporteCumplimientoDto reporte) {
        // ByteArrayOutputStream permite crear el archivo en memoria (RAM)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Título
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph titulo = new Paragraph("Reporte de Cumplimiento - Grupo Cordillera", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(Chunk.NEWLINE);

            // 2. Contenido
            Font fontTexto = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("KPI Analizado: " + reporte.getNombreKpi(), fontTexto));
            document.add(new Paragraph("Meta Establecida: $" + reporte.getMetaEstablecida(), fontTexto));
            document.add(new Paragraph("Ventas Reales: $" + reporte.getVentasReales(), fontTexto));

            // 3. Destacar el estado final
            Font fontEstado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("ESTADO ACTUAL: " + reporte.getEstado() +
                    " (" + reporte.getPorcentajeCumplimiento() + "%)", fontEstado));

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar el PDF", e);
        }

        return out.toByteArray(); // Devolvemos el PDF como un arreglo de bytes
    }
}
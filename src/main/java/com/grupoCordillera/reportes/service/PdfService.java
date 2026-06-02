package com.grupoCordillera.reportes.service;


import com.grupoCordillera.reportes.dto.ReporteCumplimientoDto;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.util.Locale;

@Service
public class PdfService {

    public byte[] generarPdfDeCumplimiento(ReporteCumplimientoDto reporte) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Configuramos el formato de moneda para Chile (CLP)
            NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));

            // 2. Título
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph titulo = new Paragraph("Reporte de Cumplimiento - Grupo Cordillera", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(Chunk.NEWLINE);

            // 3. Contenido (Usando el formateador)
            Font fontTexto = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("KPI Analizado: " + reporte.getNombreKpi(), fontTexto));

            // Aquí aplicamos el formato a la meta y a las ventas
            document.add(new Paragraph("Meta Establecida: " + formatoMoneda.format(reporte.getMetaEstablecida()), fontTexto));
            document.add(new Paragraph("Ventas Reales: " + formatoMoneda.format(reporte.getVentasReales()), fontTexto));

            // 4. Destacar el estado final
            Font fontEstado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("ESTADO ACTUAL: " + reporte.getEstado() +
                    " (" + reporte.getPorcentajeCumplimiento() + "%)", fontEstado));

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar el PDF", e);
        }

        return out.toByteArray();
    }
}
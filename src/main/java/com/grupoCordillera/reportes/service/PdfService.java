package com.grupoCordillera.reportes.service;


import com.grupoCordillera.reportes.dto.ReporteCumplimientoDto;
import com.grupoCordillera.reportes.dto.VentaDetalleDto;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.util.Locale;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import java.awt.Color;
import java.util.Map;

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
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Resumen por Sucursales", fontEstado));
            document.add(Chunk.NEWLINE);

            // --- TABLA 1: Resumen por Sucursal ---
            PdfPTable tablaSucursales = new PdfPTable(2); // 2 columnas
            tablaSucursales.setWidthPercentage(100);

            tablaSucursales.addCell(new PdfPCell(new Phrase("Sucursal", fontTexto)));
            tablaSucursales.addCell(new PdfPCell(new Phrase("Total Vendido", fontTexto)));

            for (Map.Entry<String, Double> entry : reporte.getTotalesPorSucursal().entrySet()) {
                tablaSucursales.addCell(entry.getKey());
                tablaSucursales.addCell(formatoMoneda.format(entry.getValue()));
            }
            document.add(tablaSucursales);

            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Detalle de Transacciones", fontEstado));
            document.add(Chunk.NEWLINE);

            // --- TABLA 2: Detalle de Productos Vendidos ---
            PdfPTable tablaDetalles = new PdfPTable(5); // 5 columnas
            tablaDetalles.setWidthPercentage(100);

            // Cabeceras
            String[] cabeceras = {"ID Prod.", "Producto", "Sucursal", "Cant.", "Subtotal"};
            for (String cabecera : cabeceras) {
                PdfPCell celda = new PdfPCell(new Phrase(cabecera, fontTexto));
                celda.setBackgroundColor(Color.LIGHT_GRAY);
                tablaDetalles.addCell(celda);
            }

            // Filas con datos
            for (VentaDetalleDto detalle : reporte.getDetalleVentas()) {
                tablaDetalles.addCell(String.valueOf(detalle.getProductoId()));
                tablaDetalles.addCell(detalle.getProductoNombre());
                tablaDetalles.addCell(detalle.getSucursalNombre());
                tablaDetalles.addCell(String.valueOf(detalle.getCantidad()));
                tablaDetalles.addCell(formatoMoneda.format(detalle.getMontoTotal()));
            }
            document.add(tablaDetalles);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar el PDF", e);
        }

        return out.toByteArray();

    }
}
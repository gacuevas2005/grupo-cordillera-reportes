package com.grupoCordillera.reportes.service;


 // Ajusta tu paquete si es distinto

import com.grupoCordillera.reportes.dto.ReporteCumplimientoDto;
import com.grupoCordillera.reportes.dto.VentaDetalleDto;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

@Service
public class PdfService {

    public byte[] generarPdfDeCumplimiento(ReporteCumplimientoDto reporte) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));

            // --- TÍTULO ---
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph titulo = new Paragraph("Reporte de Cumplimiento - Grupo Cordillera", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(Chunk.NEWLINE);

            // --- CABECERA DE DATOS ---
            Font fontTexto = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("KPI Analizado: " + reporte.getNombreKpi(), fontTexto));
            document.add(new Paragraph("Meta Establecida: " + formatoMoneda.format(reporte.getMetaEstablecida()), fontTexto));
            document.add(new Paragraph("Ventas Reales Totales: " + formatoMoneda.format(reporte.getVentasReales()), fontTexto));

            Font fontEstado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("ESTADO ACTUAL: " + reporte.getEstado() +
                    " (" + reporte.getPorcentajeCumplimiento() + "%)", fontEstado));
            document.add(Chunk.NEWLINE);

            // --- GENERAR E INSERTAR EL GRÁFICO DE TORTA ---
            Image graficoPdf = crearGraficoTorta(reporte.getTotalesPorSucursal());
            document.add(graficoPdf);
            document.add(Chunk.NEWLINE);

            // --- TABLA 1: Resumen por Sucursal ---
            document.add(new Paragraph("Resumen de Ventas por Sucursal", fontEstado));
            document.add(Chunk.NEWLINE);
            PdfPTable tablaSucursales = new PdfPTable(2);
            tablaSucursales.setWidthPercentage(100);
            tablaSucursales.addCell(new PdfPCell(new Phrase("Sucursal", fontTexto)));
            tablaSucursales.addCell(new PdfPCell(new Phrase("Total Vendido", fontTexto)));

            for (Map.Entry<String, Double> entry : reporte.getTotalesPorSucursal().entrySet()) {
                tablaSucursales.addCell(entry.getKey());
                tablaSucursales.addCell(formatoMoneda.format(entry.getValue()));
            }
            document.add(tablaSucursales);
            document.add(Chunk.NEWLINE);

            // --- TABLA 2: Detalle de Productos Vendidos ---
            document.add(new Paragraph("Detalle de Transacciones", fontEstado));
            document.add(Chunk.NEWLINE);
            PdfPTable tablaDetalles = new PdfPTable(5);
            tablaDetalles.setWidthPercentage(100);

            String[] cabeceras = {"ID Prod.", "Producto", "Sucursal", "Cant.", "Subtotal"};
            for (String cabecera : cabeceras) {
                PdfPCell celda = new PdfPCell(new Phrase(cabecera, fontTexto));
                celda.setBackgroundColor(Color.LIGHT_GRAY);
                tablaDetalles.addCell(celda);
            }

            for (VentaDetalleDto detalle : reporte.getDetalleVentas()) {
                tablaDetalles.addCell(String.valueOf(detalle.getProductoId()));
                tablaDetalles.addCell(detalle.getProductoNombre());
                tablaDetalles.addCell(detalle.getSucursalNombre());
                tablaDetalles.addCell(String.valueOf(detalle.getCantidad()));
                tablaDetalles.addCell(formatoMoneda.format(detalle.getMontoTotal()));
            }
            document.add(tablaDetalles);

            Image graficoBarras = crearGraficoBarras(reporte.getTopProductos());
            document.add(graficoBarras);
            document.add(Chunk.NEWLINE);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF con gráfico", e);
        }

        return out.toByteArray();
    }

    // --- MÉTODO MAGICO PARA DIBUJAR EL GRÁFICO ---
    private Image crearGraficoTorta(Map<String, Double> totales) throws Exception {
        // 1. Llenar los datos
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (Map.Entry<String, Double> entry : totales.entrySet()) {
            dataset.setValue(entry.getKey(), entry.getValue());
        }

        // 2. Crear el gráfico visualmente
        JFreeChart chart = ChartFactory.createPieChart(
                "Porcentaje de Ventas por Sucursal", // Título
                dataset,
                true, // Mostrar leyenda
                true,
                false
        );

        // 3. Formatear para que muestre el porcentaje en las etiquetas
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE); // Fondo blanco limpio
        plot.setOutlineVisible(false); // Quitar bordes feos

        // Esto hace que la etiqueta diga "Melipilla = 25%"
        PieSectionLabelGenerator labelGenerator = new StandardPieSectionLabelGenerator("{0} = {2}");
        plot.setLabelGenerator(labelGenerator);

        // 4. Transformar el gráfico de Java a una Imagen para el PDF
        BufferedImage bufferedImage = chart.createBufferedImage(500, 300);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);
        Image imagenPdf = Image.getInstance(baos.toByteArray());
        imagenPdf.setAlignment(Element.ALIGN_CENTER);

        return imagenPdf;
    }
    private Image crearGraficoBarras(Map<String, Integer> topProductos) throws Exception {
        // 1. Llenar los datos
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, Integer> entry : topProductos.entrySet()) {
            // Se le pasa: Valor, Leyenda (Agrupación), Etiqueta Eje X
            dataset.addValue(entry.getValue(), "Unidades", entry.getKey());
        }

        // 2. Crear el gráfico de barras visualmente
        JFreeChart chart = ChartFactory.createBarChart(
                "Top 10 Productos Más Vendidos", // Título
                "Producto",                      // Etiqueta Eje X
                "Cantidad Vendida",              // Etiqueta Eje Y
                dataset,
                PlotOrientation.VERTICAL,        // Orientación
                false,                           // Mostrar leyenda
                true,                            // Usar tooltips
                false                            // Usar URLs
        );

        // 3. Formatear colores para que se vea corporativo y limpio
        chart.setBackgroundPaint(Color.WHITE);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        // 4. Transformar a imagen para OpenPDF
        BufferedImage bufferedImage = chart.createBufferedImage(500, 300);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);
        Image imagenPdf = Image.getInstance(baos.toByteArray());
        imagenPdf.setAlignment(Element.ALIGN_CENTER);

        return imagenPdf;
    }
}
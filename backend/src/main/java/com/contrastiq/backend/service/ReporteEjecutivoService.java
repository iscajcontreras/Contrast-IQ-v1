package com.contrastiq.backend.service;

import com.contrastiq.backend.dto.ComparativaSedeDTO;
import com.contrastiq.backend.repository.InyeccionRepository;
import com.contrastiq.backend.util.ValidadorRangoFechas;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// "Reportes ejecutivos": comparativa de KPIs entre sedes, exportable a
// Excel para direccion / jefatura de radiologia.
@Service
@RequiredArgsConstructor
public class ReporteEjecutivoService {

    private final InyeccionRepository inyeccionRepository;

    @Transactional(readOnly = true)
    public List<ComparativaSedeDTO> comparativaSedes(LocalDateTime desde, LocalDateTime hasta) {
        ValidadorRangoFechas.validar(desde, hasta);
        List<Object[]> filas = inyeccionRepository.comparativaPorSede(desde, hasta);

        return filas.stream().map(f -> {
            Long sedeId = ((Number) f[0]).longValue();
            String sede = (String) f[1];
            Long total = ((Number) f[2]).longValue();
            BigDecimal volumen = (BigDecimal) f[3];
            Long fallidas = ((Number) f[4]).longValue();
            double tasaFalla = total > 0 ? Math.round((fallidas * 10000.0) / total) / 100.0 : 0.0;

            return ComparativaSedeDTO.builder()
                    .sedeId(sedeId)
                    .sede(sede)
                    .totalInyecciones(total)
                    .volumenTotalMl(volumen)
                    .inyeccionesFallidas(fallidas)
                    .tasaFallaPorcentaje(tasaFalla)
                    .build();
        }).toList();
    }

    // Genera el .xlsx en memoria (sin tocar disco) y lo devuelve como
    // arreglo de bytes listo para descargar desde el controller.
    public byte[] exportarComparativaSedesExcel(LocalDateTime desde, LocalDateTime hasta) {
        List<ComparativaSedeDTO> filas = comparativaSedes(desde, hasta);
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = workbook.createSheet("Comparativa por sede");

            CellStyle estiloTitulo = workbook.createCellStyle();
            Font fuenteTitulo = workbook.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 14);
            estiloTitulo.setFont(fuenteTitulo);

            CellStyle estiloEncabezado = workbook.createCellStyle();
            Font fuenteEncabezado = workbook.createFont();
            fuenteEncabezado.setBold(true);
            fuenteEncabezado.setColor(IndexedColors.WHITE.getIndex());
            estiloEncabezado.setFont(fuenteEncabezado);
            estiloEncabezado.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            int fila = 0;
            Row filaTitulo = hoja.createRow(fila++);
            Cell celdaTitulo = filaTitulo.createCell(0);
            celdaTitulo.setCellValue("Comparativa de inyecciones por sede");
            celdaTitulo.setCellStyle(estiloTitulo);

            Row filaRango = hoja.createRow(fila++);
            filaRango.createCell(0).setCellValue("Periodo: " + desde.format(formato) + " a " + hasta.format(formato));
            fila++;

            String[] encabezados = {"Sede", "Inyecciones totales", "Volumen total (ml)", "Inyecciones fallidas", "Tasa de falla (%)"};
            Row filaEncabezado = hoja.createRow(fila++);
            for (int i = 0; i < encabezados.length; i++) {
                Cell celda = filaEncabezado.createCell(i);
                celda.setCellValue(encabezados[i]);
                celda.setCellStyle(estiloEncabezado);
            }

            for (ComparativaSedeDTO c : filas) {
                Row filaDatos = hoja.createRow(fila++);
                filaDatos.createCell(0).setCellValue(c.getSede());
                filaDatos.createCell(1).setCellValue(c.getTotalInyecciones());
                filaDatos.createCell(2).setCellValue(c.getVolumenTotalMl().doubleValue());
                filaDatos.createCell(3).setCellValue(c.getInyeccionesFallidas());
                filaDatos.createCell(4).setCellValue(c.getTasaFallaPorcentaje());
            }

            for (int i = 0; i < encabezados.length; i++) {
                hoja.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo Excel", e);
        }
    }
}

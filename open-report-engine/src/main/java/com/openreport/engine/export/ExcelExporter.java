package com.openreport.engine.export;

import com.openreport.common.exception.BusinessException;
import com.openreport.engine.calcite.CalciteQueryExecutor;
import com.openreport.engine.model.ReportCell;
import com.openreport.engine.model.ReportTemplate;
import com.openreport.engine.model.RenderResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jxls.common.Context;
import org.jxls.util.JxlsHelper;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class ExcelExporter {

    public byte[] exportFromRenderResult(RenderResult renderResult) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Report");
            createTitleRow(workbook, sheet, renderResult);
            writeCellMatrix(workbook, sheet, renderResult.getCellMatrix());

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Failed to export Excel", e);
            throw new BusinessException("Failed to export Excel: " + e.getMessage());
        }
    }

    public byte[] exportWithTemplate(InputStream templateInputStream,
                                      Map<String, Object> data) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Context context = new Context();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                context.putVar(entry.getKey(), entry.getValue());
            }
            JxlsHelper.getInstance().processTemplate(templateInputStream, outputStream, context);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Failed to export Excel with template", e);
            throw new BusinessException("Failed to export Excel: " + e.getMessage());
        }
    }

    public byte[] exportDataSet(String sheetName, List<Map<String, Object>> data) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(sheetName == null ? "Data" : sheetName);
            if (data != null && !data.isEmpty()) {
                writeHeaderRow(workbook, sheet, data.get(0));
                writeDataRows(workbook, sheet, data);
                autoSizeColumns(sheet, data.get(0).size());
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Failed to export data set to Excel", e);
            throw new BusinessException("Failed to export Excel: " + e.getMessage());
        }
    }

    private void createTitleRow(Workbook workbook, Sheet sheet, RenderResult renderResult) {
        if (renderResult.getMeta() == null || !renderResult.getMeta().containsKey("templateName")) {
            return;
        }

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(renderResult.getMeta().get("templateName").toString());

        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        titleCell.setCellStyle(style);
    }

    private void writeCellMatrix(Workbook workbook, Sheet sheet, List<List<ReportCell>> cellMatrix) {
        if (cellMatrix == null || cellMatrix.isEmpty()) {
            return;
        }

        int startRow = sheet.getLastRowNum() + 1;
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        for (int i = 0; i < cellMatrix.size(); i++) {
            List<ReportCell> rowCells = cellMatrix.get(i);
            Row row = sheet.createRow(startRow + i);

            for (int j = 0; j < rowCells.size(); j++) {
                ReportCell reportCell = rowCells.get(j);
                if (reportCell != null) {
                    Cell cell = row.createCell(j);
                    Object value = reportCell.getValue();
                    setCellValue(cell, value);

                    if (i == 0) {
                        cell.setCellStyle(headerStyle);
                    } else {
                        cell.setCellStyle(dataStyle);
                    }

                    if (reportCell.getRowSpan() > 1 || reportCell.getColSpan() > 1) {
                        sheet.addMergedRegion(new CellRangeAddress(
                                startRow + i,
                                startRow + i + reportCell.getRowSpan() - 1,
                                j,
                                j + reportCell.getColSpan() - 1
                        ));
                    }
                }
            }
        }
    }

    private void writeHeaderRow(Workbook workbook, Sheet sheet, Map<String, Object> firstRow) {
        Row headerRow = sheet.createRow(0);
        CellStyle style = createHeaderStyle(workbook);

        int colIndex = 0;
        for (String key : firstRow.keySet()) {
            Cell cell = headerRow.createCell(colIndex++);
            cell.setCellValue(key);
            cell.setCellStyle(style);
        }
    }

    private void writeDataRows(Workbook workbook, Sheet sheet, List<Map<String, Object>> data) {
        CellStyle style = createDataStyle(workbook);

        for (int i = 0; i < data.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Map<String, Object> rowData = data.get(i);
            int colIndex = 0;
            for (Object value : rowData.values()) {
                Cell cell = row.createCell(colIndex++);
                setCellValue(cell, value);
                cell.setCellStyle(style);
            }
        }
    }

    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof java.util.Date) {
            cell.setCellValue((java.util.Date) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    public byte[] exportDataSetStreaming(String sheetName, int totalRowEstimate,
                                          List<String> headers, Consumer<StreamingWriter> writerConsumer) {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        workbook.setCompressTempFiles(true);
        try {
            Sheet sheet = workbook.createSheet(sheetName == null ? "Data" : sheetName);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            int rowIndex = 0;
            if (headers != null && !headers.isEmpty()) {
                Row headerRow = sheet.createRow(rowIndex++);
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers.get(i));
                    cell.setCellStyle(headerStyle);
                }
            }

            final int startRow = rowIndex;
            StreamingWriter writer = new StreamingWriter(sheet, dataStyle, startRow);
            writerConsumer.accept(writer);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Failed to export streaming Excel", e);
            throw new BusinessException("Failed to export streaming Excel: " + e.getMessage());
        } finally {
            workbook.dispose();
            try {
                workbook.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static class StreamingWriter {
        private final Sheet sheet;
        private final CellStyle dataStyle;
        private int rowIndex;
        private int totalRows;

        public StreamingWriter(Sheet sheet, CellStyle dataStyle, int startRow) {
            this.sheet = sheet;
            this.dataStyle = dataStyle;
            this.rowIndex = startRow;
            this.totalRows = 0;
        }

        public void writeRow(Map<String, Object> rowData) {
            if (rowData == null) return;
            writeRow(new ArrayList<>(rowData.values()));
        }

        public void writeRow(List<?> values) {
            if (values == null) return;
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < values.size(); i++) {
                Cell cell = row.createCell(i);
                setCellValue(cell, values.get(i));
                cell.setCellStyle(dataStyle);
            }
            totalRows++;
        }

        public void writeBatch(List<Map<String, Object>> batch) {
            if (batch == null || batch.isEmpty()) return;
            for (Map<String, Object> row : batch) {
                writeRow(row);
            }
        }

        public int getTotalRows() {
            return totalRows;
        }
    }

    public byte[] exportDataSetBatch(String sheetName, CalciteQueryExecutor.BatchCallback dataProvider) {
        return exportDataSetBatch(sheetName, 1000, dataProvider);
    }

    public byte[] exportDataSetBatch(String sheetName, int batchSize,
                                      CalciteQueryExecutor.BatchCallback dataProvider) {
        SXSSFWorkbook workbook = new SXSSFWorkbook(batchSize);
        workbook.setCompressTempFiles(true);
        try {
            Sheet sheet = workbook.createSheet(sheetName == null ? "Data" : sheetName);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            final boolean[] headerWritten = {false};
            final int[] rowIndex = {0};

            dataProvider.onBatch(null, 0, 0);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Failed to export batch Excel", e);
            throw new BusinessException("Failed to export batch Excel: " + e.getMessage());
        } finally {
            workbook.dispose();
            try {
                workbook.close();
            } catch (IOException ignored) {
            }
        }
    }
}

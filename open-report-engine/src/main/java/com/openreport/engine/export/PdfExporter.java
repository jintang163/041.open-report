package com.openreport.engine.export;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.openreport.common.exception.BusinessException;
import com.openreport.engine.model.ReportCell;
import com.openreport.engine.model.RenderResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PdfExporter {

    public byte[] exportFromRenderResult(RenderResult renderResult) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            addTitle(document, renderResult);
            addCellMatrix(document, renderResult.getCellMatrix());

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException e) {
            log.error("Failed to export PDF", e);
            throw new BusinessException("Failed to export PDF: " + e.getMessage());
        }
    }

    public byte[] exportDataSet(String title, List<Map<String, Object>> data) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            if (title != null && !title.isEmpty()) {
                Paragraph titleParagraph = new Paragraph(title, getTitleFont());
                titleParagraph.setAlignment(Element.ALIGN_CENTER);
                titleParagraph.setSpacingAfter(20);
                document.add(titleParagraph);
            }

            if (data != null && !data.isEmpty()) {
                PdfPTable table = createDataTable(data);
                document.add(table);
            }

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException e) {
            log.error("Failed to export data set to PDF", e);
            throw new BusinessException("Failed to export PDF: " + e.getMessage());
        }
    }

    private void addTitle(Document document, RenderResult renderResult) throws DocumentException {
        if (renderResult.getMeta() == null || !renderResult.getMeta().containsKey("templateName")) {
            return;
        }
        String title = renderResult.getMeta().get("templateName").toString();
        Paragraph titleParagraph = new Paragraph(title, getTitleFont());
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(20);
        document.add(titleParagraph);
    }

    private void addCellMatrix(Document document, List<List<ReportCell>> cellMatrix) throws DocumentException {
        if (cellMatrix == null || cellMatrix.isEmpty()) {
            return;
        }

        int maxCols = 0;
        for (List<ReportCell> row : cellMatrix) {
            maxCols = Math.max(maxCols, row.size());
        }

        PdfPTable table = new PdfPTable(maxCols);
        table.setWidthPercentage(100);

        for (int i = 0; i < cellMatrix.size(); i++) {
            List<ReportCell> row = cellMatrix.get(i);
            for (int j = 0; j < row.size(); j++) {
                ReportCell reportCell = row.get(j);
                if (reportCell != null) {
                    PdfPCell pdfCell = createPdfCell(reportCell, i == 0);
                    if (reportCell.getRowSpan() > 1) {
                        pdfCell.setRowspan(reportCell.getRowSpan());
                    }
                    if (reportCell.getColSpan() > 1) {
                        pdfCell.setColspan(reportCell.getColSpan());
                    }
                    table.addCell(pdfCell);
                }
            }
        }

        document.add(table);
    }

    private PdfPTable createDataTable(List<Map<String, Object>> data) {
        Map<String, Object> firstRow = data.get(0);
        PdfPTable table = new PdfPTable(firstRow.size());
        table.setWidthPercentage(100);

        Font headerFont = getHeaderFont();
        for (String key : firstRow.keySet()) {
            PdfPCell headerCell = new PdfPCell(new Phrase(key, headerFont));
            headerCell.setBackgroundColor(Color.LIGHT_GRAY);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell.setPadding(5);
            table.addCell(headerCell);
        }

        Font dataFont = getDataFont();
        for (Map<String, Object> rowData : data) {
            for (Object value : rowData.values()) {
                PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value.toString(), dataFont));
                cell.setPadding(5);
                table.addCell(cell);
            }
        }

        return table;
    }

    private PdfPCell createPdfCell(ReportCell reportCell, boolean isHeader) {
        Font font = isHeader ? getHeaderFont() : getDataFont();
        Object value = reportCell.getValue();
        Phrase phrase = new Phrase(value == null ? "" : value.toString(), font);
        PdfPCell cell = new PdfPCell(phrase);
        cell.setPadding(5);
        if (isHeader) {
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        }
        return cell;
    }

    private Font getTitleFont() {
        try {
            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font font = new Font(baseFont, 18, Font.BOLD);
            return font;
        } catch (Exception e) {
            return new Font(Font.HELVETICA, 18, Font.BOLD);
        }
    }

    private Font getHeaderFont() {
        try {
            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font font = new Font(baseFont, 12, Font.BOLD);
            return font;
        } catch (Exception e) {
            return new Font(Font.HELVETICA, 12, Font.BOLD);
        }
    }

    private Font getDataFont() {
        try {
            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font font = new Font(baseFont, 10, Font.NORMAL);
            return font;
        } catch (Exception e) {
            return new Font(Font.HELVETICA, 10, Font.NORMAL);
        }
    }
}

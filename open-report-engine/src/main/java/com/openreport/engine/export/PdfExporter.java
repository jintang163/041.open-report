package com.openreport.engine.export;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.openreport.common.exception.BusinessException;
import com.openreport.engine.model.ReportCell;
import com.openreport.engine.model.RenderResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PdfExporter {

    @Data
    public static class WatermarkInfo {
        private String username;
        private String timestamp;
        private String ip;
        private float opacity = 0.15f;
        private int fontSize = 14;
        private float rotate = -22f;
        private int gapX = 180;
        private int gapY = 100;
        private String color = "#666666";

        public WatermarkInfo() {
            this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        }

        public WatermarkInfo(String username, String ip) {
            this.username = username;
            this.ip = ip;
            this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        }
    }

    private static class WatermarkPageEvent extends PdfPageEventHelper {
        private final WatermarkInfo watermarkInfo;
        private BaseFont baseFont;

        public WatermarkPageEvent(WatermarkInfo watermarkInfo) {
            this.watermarkInfo = watermarkInfo;
            try {
                this.baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                try {
                    this.baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                } catch (Exception ex) {
                    this.baseFont = null;
                }
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            if (watermarkInfo == null || baseFont == null) {
                return;
            }

            PdfContentByte contentByte = writer.getDirectContentUnder();

            float pageWidth = document.getPageSize().getWidth();
            float pageHeight = document.getPageSize().getHeight();
            float marginLeft = document.leftMargin();
            float marginRight = document.rightMargin();
            float marginTop = document.topMargin();
            float marginBottom = document.bottomMargin();

            float contentWidth = pageWidth - marginLeft - marginRight;
            float contentHeight = pageHeight - marginTop - marginBottom;

            String line1 = watermarkInfo.getUsername() != null ? watermarkInfo.getUsername() : "匿名用户";
            String line2 = watermarkInfo.getTimestamp() != null ? watermarkInfo.getTimestamp() : "";
            String line3 = watermarkInfo.getIp() != null ? "IP: " + watermarkInfo.getIp() : "";

            int fontSize = watermarkInfo.getFontSize();
            float lineHeight = fontSize * 1.4f;
            float blockWidth = Math.max(watermarkInfo.getGapX(), 200f);
            float blockHeight = Math.max(watermarkInfo.getGapY(), lineHeight * 3 + 40);

            double radians = Math.toRadians(watermarkInfo.getRotate());

            int colorInt = Color.decode(watermarkInfo.getColor()).getRGB();
            BaseColor baseColor = new BaseColor(
                    (colorInt >> 16) & 0xFF,
                    (colorInt >> 8) & 0xFF,
                    colorInt & 0xFF
            );

            PdfGState gs = new PdfGState();
            gs.setFillOpacity(watermarkInfo.getOpacity());
            gs.setStrokeOpacity(watermarkInfo.getOpacity());

            contentByte.saveState();
            contentByte.setGState(gs);
            contentByte.beginText();
            contentByte.setFontAndSize(baseFont, fontSize);
            contentByte.setColorFill(baseColor);

            int cols = (int) Math.ceil(contentWidth / blockWidth) + 2;
            int rows = (int) Math.ceil(contentHeight / blockHeight) + 2;

            float startX = marginLeft - blockWidth;
            float startY = marginBottom - blockHeight;

            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    float centerX = startX + col * blockWidth + blockWidth / 2;
                    float centerY = startY + row * blockHeight + blockHeight / 2;

                    drawRotatedText(contentByte, line1, centerX, centerY + lineHeight, radians);
                    drawRotatedText(contentByte, line2, centerX, centerY, radians);
                    drawRotatedText(contentByte, line3, centerX, centerY - lineHeight, radians);
                }
            }

            contentByte.endText();
            contentByte.restoreState();
        }

        private void drawRotatedText(PdfContentByte contentByte, String text,
                                      float x, float y, double radians) {
            if (text == null || text.isEmpty()) {
                return;
            }
            float textWidth = baseFont.getWidthPoint(text, watermarkInfo.getFontSize());
            float cos = (float) Math.cos(radians);
            float sin = (float) Math.sin(radians);
            float adjustedX = x - (textWidth / 2) * cos;
            float adjustedY = y + (textWidth / 2) * sin;

            contentByte.showTextAligned(
                    Element.ALIGN_CENTER,
                    text,
                    x,
                    y,
                    watermarkInfo.getRotate()
            );
        }
    }

    public byte[] exportFromRenderResult(RenderResult renderResult) {
        return exportFromRenderResult(renderResult, null);
    }

    public byte[] exportFromRenderResult(RenderResult renderResult, WatermarkInfo watermarkInfo) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);

            if (watermarkInfo != null) {
                writer.setPageEvent(new WatermarkPageEvent(watermarkInfo));
            }

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
        return exportDataSet(title, data, null);
    }

    public byte[] exportDataSet(String title, List<Map<String, Object>> data, WatermarkInfo watermarkInfo) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);

            if (watermarkInfo != null) {
                writer.setPageEvent(new WatermarkPageEvent(watermarkInfo));
            }

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
            return new Font(baseFont, 18, Font.BOLD);
        } catch (Exception e) {
            return new Font(Font.HELVETICA, 18, Font.BOLD);
        }
    }

    private Font getHeaderFont() {
        try {
            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            return new Font(baseFont, 12, Font.BOLD);
        } catch (Exception e) {
            return new Font(Font.HELVETICA, 12, Font.BOLD);
        }
    }

    private Font getDataFont() {
        try {
            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            return new Font(baseFont, 10, Font.NORMAL);
        } catch (Exception e) {
            return new Font(Font.HELVETICA, 10, Font.NORMAL);
        }
    }
}

package com.openreport.engine.renderer;

import com.openreport.engine.model.ReportCell;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HtmlTableRenderer {

    public String render(List<List<ReportCell>> cellMatrix) {
        if (cellMatrix == null || cellMatrix.isEmpty()) {
            return "<table></table>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"report-table\" style=\"border-collapse: collapse; width: 100%;\">");

        for (List<ReportCell> row : cellMatrix) {
            sb.append("<tr>");
            for (ReportCell cell : row) {
                if (cell != null) {
                    renderCell(sb, cell);
                }
            }
            sb.append("</tr>");
        }

        sb.append("</table>");
        return sb.toString();
    }

    private void renderCell(StringBuilder sb, ReportCell cell) {
        sb.append("<td");

        if (cell.getRowSpan() != null && cell.getRowSpan() > 1) {
            sb.append(" rowspan=\"").append(cell.getRowSpan()).append("\"");
        }
        if (cell.getColSpan() != null && cell.getColSpan() > 1) {
            sb.append(" colspan=\"").append(cell.getColSpan()).append("\"");
        }

        String style = buildStyle(cell);
        if (style != null && !style.isEmpty()) {
            sb.append(" style=\"").append(style).append("\"");
        }

        sb.append(">");

        Object value = cell.getValue();
        if (value != null) {
            sb.append(escapeHtml(value.toString()));
        }

        sb.append("</td>");
    }

    private String buildStyle(ReportCell cell) {
        StringBuilder sb = new StringBuilder();
        sb.append("border: 1px solid #ddd; padding: 8px;");

        if (cell.getStyles() != null && !cell.getStyles().isEmpty()) {
            for (Map.Entry<String, Object> entry : cell.getStyles().entrySet()) {
                sb.append(convertCssProperty(entry.getKey())).append(": ")
                        .append(entry.getValue()).append(";");
            }
        }

        return sb.toString();
    }

    private String convertCssProperty(String property) {
        if (property == null || property.isEmpty()) {
            return property;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < property.length(); i++) {
            char c = property.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append("-").append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

package com.attendx.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class CsvExporter {

    public static void exportToFile(File file, String[] headers, List<String[]> dataRows) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            // Write Header
            if (headers != null && headers.length > 0) {
                pw.println(formatCsvRow(headers));
            }
            // Write Data
            if (dataRows != null) {
                for (String[] row : dataRows) {
                    pw.println(formatCsvRow(row));
                }
            }
        }
    }

    private static String formatCsvRow(String[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            String val = values[i] != null ? values[i] : "";
            if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                val = "\"" + val.replace("\"", "\"\"") + "\"";
            }
            sb.append(val);
            if (i < values.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    private CsvExporter() {}
}

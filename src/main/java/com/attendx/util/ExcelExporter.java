package com.attendx.util;

import com.attendx.model.Student;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExcelExporter {

    public static void exportStudents(File file, List<Student> students) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Students");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Create Header Row
            String[] headers = {"Reg No", "First Name", "Last Name", "Email", "Phone", "Department", "Course", "Semester", "Section", "Academic Year"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            int rowIdx = 1;
            for (Student s : students) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(s.getRegistrationNumber());
                row.createCell(1).setCellValue(s.getFirstName());
                row.createCell(2).setCellValue(s.getLastName());
                row.createCell(3).setCellValue(s.getEmail());
                row.createCell(4).setCellValue(s.getPhone() != null ? s.getPhone() : "");
                row.createCell(5).setCellValue(s.getDepartmentName() != null ? s.getDepartmentName() : "");
                row.createCell(6).setCellValue(s.getCourseName() != null ? s.getCourseName() : "");
                row.createCell(7).setCellValue(s.getSemesterLabel() != null ? s.getSemesterLabel() : "");
                row.createCell(8).setCellValue(s.getSection() != null ? s.getSection() : "A");
                row.createCell(9).setCellValue(s.getAcademicYear() != null ? s.getAcademicYear() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }

    public static void exportReport(File file, String sheetName, String[] headers, List<String[]> rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName != null ? sheetName : "Report");

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (String[] r : rows) {
                Row row = sheet.createRow(rowIdx++);
                for (int col = 0; col < r.length; col++) {
                    row.createCell(col).setCellValue(r[col] != null ? r[col] : "");
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }

    public static List<Student> importStudentsFromExcel(File file, int defaultDeptId, int defaultCourseId, int defaultSemId, String academicYear) throws IOException {
        List<Student> students = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file); Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String regNo = getCellString(row.getCell(0));
                String firstName = getCellString(row.getCell(1));
                String lastName = getCellString(row.getCell(2));
                String email = getCellString(row.getCell(3));
                String phone = getCellString(row.getCell(4));
                String section = getCellString(row.getCell(8));
                if (section.isEmpty()) section = "A";

                if (!regNo.isEmpty() && !firstName.isEmpty() && !email.isEmpty()) {
                    Student s = new Student(0, regNo, firstName, lastName, email, phone, defaultDeptId, defaultCourseId, defaultSemId, section, academicYear);
                    students.add(s);
                }
            }
        }
        return students;
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    private ExcelExporter() {}
}

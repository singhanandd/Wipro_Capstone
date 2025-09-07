package utils;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Creates an Excel "Results_Index.xlsx" inside the per-run screenshots folder.
 * Columns: Timestamp | TestName | Status | Screenshot (hyperlink)
 *
 * Usage (already shown in your BaseTest):
 *   ResultsWorkbook.start(runFolder);
 *   ResultsWorkbook.log(testName, status, screenshotPath);
 *   ResultsWorkbook.finish();
 */
public final class ResultsWorkbook {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static Workbook wb;           // <-- interface (close() throws IOException)
    private static Sheet sheet;
    private static int nextRow = 0;

    private static String runFolder;
    private static File outFile;

    private ResultsWorkbook() {}

    /** Create the run folder (if needed) and start a fresh workbook. */
    public static synchronized void start(String runFolderPath) {
        try {
            runFolder = runFolderPath;
            File dir = new File(runFolder);
            if (!dir.exists() && !dir.mkdirs()) {
                System.out.println("⚠️ Could not create run folder: " + dir.getAbsolutePath());
            }

            outFile = new File(dir, "Results_Index.xlsx");

            wb = new XSSFWorkbook();
            sheet = wb.createSheet("Results");
            nextRow = 0;

            // Header row with a simple bold style
            Row header = sheet.createRow(nextRow++);
            CellStyle hdrStyle = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            hdrStyle.setFont(bold);

            String[] headers = {"Timestamp", "TestName", "Status", "Screenshot"};
            for (int c = 0; c < headers.length; c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(headers[c]);
                cell.setCellStyle(hdrStyle);
            }

        } catch (Exception e) {
            System.out.println("⚠️ ResultsWorkbook.start() error:");
            e.printStackTrace();
        }
    }

    /** Append one row for a test. screenshotPath can be null. */
    public static synchronized void log(String testName, String status, String screenshotPath) {
        if (wb == null || sheet == null) return;

        try {
            Row row = sheet.createRow(nextRow++);
            row.createCell(0).setCellValue(LocalDateTime.now().format(TS));
            row.createCell(1).setCellValue(nullToEmpty(testName));
            row.createCell(2).setCellValue(nullToEmpty(status));

            // Screenshot cell with hyperlink if path present
            Cell shotCell = row.createCell(3);
            if (screenshotPath != null && !screenshotPath.isBlank()) {
                try {
                    // Make a relative path from the Excel file's parent to the screenshot
                    Path excelParent = Paths.get(outFile.getParentFile().getAbsolutePath());
                    Path shot = Paths.get(screenshotPath).toAbsolutePath();
                    String rel = excelParent.relativize(shot).toString();

                    CreationHelper helper = wb.getCreationHelper();
                    Hyperlink link = helper.createHyperlink(HyperlinkType.FILE);
                    link.setAddress(rel);

                    shotCell.setHyperlink(link);
                    shotCell.setCellValue("Open screenshot");
                } catch (Exception e) {
                    // Fallback: just write the path as text
                    shotCell.setCellValue(screenshotPath);
                }
            } else {
                shotCell.setCellValue("");
            }

        } catch (Exception e) {
            System.out.println("⚠️ ResultsWorkbook.log() error:");
            e.printStackTrace();
        }
    }

    /** Auto-size columns, write file to disk, and close the workbook. */
    public static synchronized void finish() {
        if (wb == null || sheet == null) return;

        FileOutputStream fos = null;
        try {
            // Auto-size columns (0..3)
            for (int c = 0; c <= 3; c++) {
                sheet.autoSizeColumn(c);
            }

            fos = new FileOutputStream(outFile);
            wb.write(fos);
        } catch (Exception e) {
            System.out.println("⚠️ ResultsWorkbook.finish() write error:");
            e.printStackTrace();
        } finally {
            // Close stream first
            if (fos != null) {
                try { fos.close(); } catch (IOException ignored) {}
            }
            // Close workbook (Workbook.close() throws IOException)
            try {
                wb.close();
            } catch (IOException ignored) {}
            wb = null;
            sheet = null;
        }
    }

    private static String nullToEmpty(String s) {
        return (s == null) ? "" : s;
    }
}

package utils;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Thread-safe helper that writes one Excel row per test:
 *   | Test | Status | Screenshot |
 *
 * File is created at: test-output/TestResults.xlsx
 */
public final class ResultsExcelWriter {

    private static final Object LOCK = new Object();

    private static Workbook wb;
    private static Sheet sheet;
    private static int nextRowIndex = 1; // 0 = header row
    private static File outFile;

    // cached hyperlink style
    private static CellStyle hyperlinkStyle;

    private ResultsExcelWriter() {}

    /** Call once (e.g., in @BeforeSuite). Creates file + header row. */
    public static void init() {
        synchronized (LOCK) {
            if (wb != null) return; // already initialized

            try {
                // ensure folder exists
                File outDir = new File("test-output");
                if (!outDir.exists()) outDir.mkdirs();

                outFile = new File(outDir, "TestResults.xlsx");

                wb = new XSSFWorkbook();
                sheet = wb.createSheet("Results");

                // header
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Test");
                header.createCell(1).setCellValue("Status");
                header.createCell(2).setCellValue("Screenshot");

                // style for screenshot links
                Font linkFont = wb.createFont();
                linkFont.setUnderline(Font.U_SINGLE);
                linkFont.setColor(IndexedColors.BLUE.getIndex());
                hyperlinkStyle = wb.createCellStyle();
                hyperlinkStyle.setFont(linkFont);

                autosize();
                // write an empty workbook with header immediately (helps if build is aborted)
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    wb.write(fos);
                }
            } catch (IOException e) {
                throw new RuntimeException("Unable to init ResultsExcelWriter", e);
            }
        }
    }

    /**
     * Append one row. Safe to call from any thread.
     * @param testName   TestNG method name (or any label)
     * @param status     PASS / FAIL / SKIP
     * @param screenshot Relative or absolute path to the .png (can be null)
     */
    public static void append(String testName, String status, String screenshot) {
        synchronized (LOCK) {
            if (wb == null || sheet == null) {
                // guard in case init() was forgotten
                init();
            }

            Row row = sheet.createRow(nextRowIndex++);
            row.createCell(0).setCellValue(testName);
            row.createCell(1).setCellValue(status);

            if (screenshot != null && !screenshot.isBlank()) {
                Cell cell = row.createCell(2);
                cell.setCellValue(screenshot);

                CreationHelper helper = wb.getCreationHelper();
                Hyperlink link = helper.createHyperlink(HyperlinkType.FILE);
                // Excel is OK with relative paths; absolute also fine
                link.setAddress(screenshot.replace('\\', '/'));
                cell.setHyperlink(link);
                cell.setCellStyle(hyperlinkStyle);
            }

            autosize();

            // write partial updates so you can open while the run is ongoing
            flushQuietly();
        }
    }

    /** Call once (e.g., in @AfterSuite). */
    public static void close() {
        synchronized (LOCK) {
            if (wb == null) return;
            flushQuietly(); // ensure latest rows on disk
            try {
                wb.close();
            } catch (IOException ignored) {}
            wb = null;
            sheet = null;
        }
    }

    private static void autosize() {
        if (sheet != null) {
            for (int c = 0; c <= 2; c++) {
                sheet.autoSizeColumn(c, true);
            }
        }
    }

    private static void flushQuietly() {
        if (wb == null || outFile == null) return;
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            wb.write(fos);
        } catch (IOException ignored) {}
    }
}

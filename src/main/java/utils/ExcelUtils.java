package utils;

import org.apache.poi.ss.usermodel.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ExcelUtils implements Closeable {
    private final Workbook workbook;
    private final Sheet sheet;
    private final DataFormatter formatter = new DataFormatter();

    public ExcelUtils(InputStream file, String sheetName) throws IOException {
        workbook = WorkbookFactory.create(file);
        Sheet s = (sheetName != null) ? workbook.getSheet(sheetName) : null;
        if (s == null) s = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
        if (s == null) throw new IllegalArgumentException("No sheets found in workbook.");
        sheet = s;
    }

    public int getRowCount() { return sheet.getLastRowNum() + 1; }

    public String getCellData(int row, int col) {
        Row r = sheet.getRow(row);
        if (r == null) return "";
        Cell c = r.getCell(col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        return formatter.formatCellValue(c);
    }

    public List<String> readColumnAsList(int col, boolean skipHeader) {
        int start = skipHeader ? 1 : 0;
        int last = sheet.getLastRowNum();
        List<String> out = new ArrayList<>();
        for (int r = start; r <= last; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell == null) continue;
            String val = formatter.formatCellValue(cell).trim();
            if (!val.isEmpty()) out.add(val);
        }
        return out;
    }

    @Override public void close() throws IOException { workbook.close(); }
}

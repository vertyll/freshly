package com.vertyll.freshly.translation.infrastructure.spreadsheet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.RecordFormatException;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.translation.application.command.ImportedTranslation;
import com.vertyll.freshly.translation.application.dto.TranslationEntry;
import com.vertyll.freshly.translation.domain.error.TranslationError;

import static java.util.Objects.requireNonNull;

@Component
public class TranslationSpreadsheet {
    public static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String FILE_NAME = "translations.xlsx";

    private static final String SHEET_NAME = "translations";
    private static final int KEY_COLUMN = 0;
    private static final int CONTEXT_COLUMN = 1;
    private static final int FIRST_LANGUAGE_COLUMN = 2;

    private static final int ROW_WINDOW = 200;
    private static final int KEY_COLUMN_WIDTH = 48 * 256;
    private static final int TEXT_COLUMN_WIDTH = 60 * 256;

    private static final int MAX_ROWS = 20_000;

    public byte[] write(List<String> headers, List<String> languages, List<TranslationEntry> rows) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(ROW_WINDOW);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(SHEET_NAME);
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle overrideStyle = overrideStyle(workbook);

            writeHeader(sheet, headers, headerStyle);
            writeRows(sheet, languages, rows, overrideStyle);

            sheet.setColumnWidth(KEY_COLUMN, KEY_COLUMN_WIDTH);
            for (int column = 0; column < languages.size(); column++) {
                sheet.setColumnWidth(FIRST_LANGUAGE_COLUMN + column, TEXT_COLUMN_WIDTH);
            }
            sheet.createFreezePane(FIRST_LANGUAGE_COLUMN, 1);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<ImportedTranslation> read(InputStream input, List<String> languages) {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new DomainException(TranslationError.IMPORT_UNREADABLE, Map.of());
            }
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getLastRowNum() > MAX_ROWS) {
                throw new DomainException(
                    TranslationError.IMPORT_TOO_LARGE,
                    Map.of("rows", sheet.getLastRowNum(), "limit", MAX_ROWS)
                );
            }
            return readRows(sheet, languages);
        } catch (IOException | NotOfficeXmlFileException | RecordFormatException unreadable) {
            throw new DomainException(TranslationError.IMPORT_UNREADABLE, Map.of(), unreadable);
        }
    }

    private static void writeHeader(Sheet sheet, List<String> headers, CellStyle style) {
        Row header = sheet.createRow(0);
        for (int column = 0; column < headers.size(); column++) {
            Cell cell = header.createCell(column);
            cell.setCellValue(headers.get(column));
            cell.setCellStyle(style);
        }
    }

    private static void writeRows(
        Sheet sheet,
        List<String> languages,
        List<TranslationEntry> rows,
        CellStyle overrideStyle
    ) {
        for (int index = 0; index < rows.size(); index++) {
            TranslationEntry entry = rows.get(index);
            Row row = sheet.createRow(index + 1);

            row.createCell(KEY_COLUMN).setCellValue(entry.key());
            row.createCell(CONTEXT_COLUMN).setCellValue(entry.context());

            for (int column = 0; column < languages.size(); column++) {
                TranslationEntry.LanguageValue value = requireNonNull(
                    entry.values().get(languages.get(column)),
                    "No value for language " + languages.get(column) + " in " + entry.key()
                );
                Cell cell = row.createCell(FIRST_LANGUAGE_COLUMN + column);

                String text = value.overrideText() == null ? value.defaultText() : value.overrideText();
                cell.setCellValue(text == null ? "" : text);
                if (value.overrideText() != null) {
                    cell.setCellStyle(overrideStyle);
                }
            }
        }
    }

    private static List<ImportedTranslation> readRows(Sheet sheet, List<String> languages) {
        DataFormatter formatter = new DataFormatter();
        List<ImportedTranslation> rows = new ArrayList<>();

        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                rows.addAll(translationsOf(formatter, row, rowIndex + 1, languages));
            }
        }
        return rows;
    }

    private static List<ImportedTranslation> translationsOf(
        DataFormatter formatter,
        Row row,
        int rowNumber,
        List<String> languages
    ) {
        String key = stringAt(formatter, row, KEY_COLUMN);
        if (key.isEmpty()) {
            return List.of();
        }
        List<ImportedTranslation> translations = new ArrayList<>();
        for (int column = 0; column < languages.size(); column++) {
            String text = stringAt(formatter, row, FIRST_LANGUAGE_COLUMN + column);
            if (!text.isEmpty()) {
                translations.add(new ImportedTranslation(key, languages.get(column), text, rowNumber));
            }
        }
        return translations;
    }

    private static String stringAt(DataFormatter formatter, Row row, int column) {
        Cell cell = row.getCell(column);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }
        return formatter.formatCellValue(cell).trim();
    }

    private static CellStyle headerStyle(Workbook workbook) {
        Font bold = workbook.createFont();
        bold.setBold(true);

        CellStyle style = workbook.createCellStyle();
        style.setFont(bold);
        return style;
    }

    private static CellStyle overrideStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}

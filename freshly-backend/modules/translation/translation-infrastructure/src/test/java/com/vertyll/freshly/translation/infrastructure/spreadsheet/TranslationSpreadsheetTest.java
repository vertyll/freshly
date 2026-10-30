package com.vertyll.freshly.translation.infrastructure.spreadsheet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.translation.application.command.ImportedTranslation;
import com.vertyll.freshly.translation.application.dto.TranslationEntry;
import com.vertyll.freshly.translation.application.dto.TranslationEntry.LanguageValue;
import com.vertyll.freshly.translation.domain.error.TranslationError;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TranslationSpreadsheetTest {
    private static final List<String> LANGUAGES = List.of("en", "pl");
    private static final List<String> HEADERS = List.of("key", "context", "en", "pl");

    private final TranslationSpreadsheet spreadsheet = new TranslationSpreadsheet();

    private static LanguageValue byDefault(String text) {
        return new LanguageValue(text, null, null, null, false);
    }

    private static TranslationEntry entry(String key, LanguageValue english, LanguageValue polish) {
        return new TranslationEntry(key, "web", Map.of("en", english, "pl", polish), false, 1L);
    }

    private List<ImportedTranslation> reread(TranslationEntry... entries) {
        byte[] written = spreadsheet.write(HEADERS, LANGUAGES, List.of(entries));
        return spreadsheet.read(new ByteArrayInputStream(written), LANGUAGES);
    }

    private static byte[] workbook(int... rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("translations");
            for (int row : rows) {
                sheet.createRow(row).createCell(0).setCellValue("error.key");
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void whatIsWrittenIsReadBackWithItsRowNumbers() {
        List<ImportedTranslation> rows = reread(entry("a.key", byDefault("Hello"), byDefault("Witaj")));

        assertThat(rows).containsExactly(
            new ImportedTranslation("a.key", "en", "Hello", 2),
            new ImportedTranslation("a.key", "pl", "Witaj", 2)
        );
    }

    @Test
    void anOverrideIsWrittenInsteadOfTheDefault() {
        LanguageValue overridden = new LanguageValue("Hello", "Hi", "editor", null, false);

        List<ImportedTranslation> rows = reread(entry("a.key", overridden, byDefault("Witaj")));

        assertThat(rows).extracting(ImportedTranslation::text).containsExactly("Hi", "Witaj");
    }

    @Test
    void aLanguageWithoutTextGivesNoRow() {
        LanguageValue empty = new LanguageValue(null, null, null, null, false);

        List<ImportedTranslation> rows = reread(entry("a.key", byDefault("Hello"), empty));

        assertThat(rows).extracting(ImportedTranslation::language).containsExactly("en");
    }

    @Test
    void cellsAreTrimmedAndNumbersReadAsText() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Row row = workbook.createSheet().createRow(1);
            row.createCell(0).setCellValue("  a.key  ");
            row.createCell(2).setCellValue(12);
            workbook.write(out);

            List<ImportedTranslation> rows =
                    spreadsheet.read(new ByteArrayInputStream(out.toByteArray()), List.of("en"));

            assertThat(rows).containsExactly(new ImportedTranslation("a.key", "en", "12", 2));
        }
    }

    @Test
    void rowsWithoutAKeyAndGapsAreSkipped() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            sheet.createRow(1).createCell(2).setCellValue("no key");
            Row kept = sheet.createRow(3);
            kept.createCell(0).setCellValue("a.key");
            kept.createCell(2).setCellValue("Hello");
            workbook.write(out);

            List<ImportedTranslation> rows =
                    spreadsheet.read(new ByteArrayInputStream(out.toByteArray()), List.of("en"));

            assertThat(rows).containsExactly(new ImportedTranslation("a.key", "en", "Hello", 4));
        }
    }

    @Test
    void aSheetOverTheLimitIsRefused() throws IOException {
        byte[] tooLarge = workbook(20_001);

        assertThatThrownBy(() -> spreadsheet.read(new ByteArrayInputStream(tooLarge), LANGUAGES))
            .isInstanceOfSatisfying(
                DomainException.class,
                e -> assertThat(e.error()).isEqualTo(TranslationError.IMPORT_TOO_LARGE)
            );
    }

    @Test
    void theLastRowAllowedIsRead() throws IOException {
        assertThat(spreadsheet.read(new ByteArrayInputStream(workbook(20_000)), LANGUAGES)).isEmpty();
    }

    @Test
    void aFileThatIsNoWorkbookIsRefused() {
        byte[] text = "key,text".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> spreadsheet.read(new ByteArrayInputStream(text), LANGUAGES)).isInstanceOfSatisfying(
            DomainException.class,
            e -> assertThat(e.error()).isEqualTo(TranslationError.IMPORT_UNREADABLE)
        );
    }

    @Test
    void aWorkbookWithoutSheetsIsRefused() throws IOException {
        byte[] noSheets;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            noSheets = out.toByteArray();
        }

        assertThatThrownBy(() -> spreadsheet.read(new ByteArrayInputStream(noSheets), LANGUAGES))
            .isInstanceOfSatisfying(
                DomainException.class,
                e -> assertThat(e.error()).isEqualTo(TranslationError.IMPORT_UNREADABLE)
            );
    }
}

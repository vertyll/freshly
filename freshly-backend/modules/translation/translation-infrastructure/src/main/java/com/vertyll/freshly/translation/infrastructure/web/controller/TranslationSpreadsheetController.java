package com.vertyll.freshly.translation.infrastructure.web.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vertyll.freshly.translation.application.command.ImportTranslationsCommand;
import com.vertyll.freshly.translation.application.command.ImportedTranslation;
import com.vertyll.freshly.translation.application.dto.ImportReport;
import com.vertyll.freshly.translation.application.port.inbound.command.TranslationImportUseCase;
import com.vertyll.freshly.translation.application.port.inbound.query.TranslationExportUseCase;
import com.vertyll.freshly.translation.application.port.inbound.query.TranslationQueryUseCase;
import com.vertyll.freshly.translation.application.security.TranslationPermission;
import com.vertyll.freshly.translation.infrastructure.resolver.CachedTranslationBundles;
import com.vertyll.freshly.translation.infrastructure.resolver.StoredTranslationResolver;
import com.vertyll.freshly.translation.infrastructure.spreadsheet.TranslationSpreadsheet;
import com.vertyll.freshly.web.i18n.MessageResolver;
import com.vertyll.freshly.web.security.RequirePermission;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/translations")
@RequiredArgsConstructor
public class TranslationSpreadsheetController {
    private static final String KEY_COLUMN_LABEL = "translation.export.column.key";
    private static final String CONTEXT_COLUMN_LABEL = "translation.export.column.context";

    private final TranslationExportUseCase exports;
    private final TranslationImportUseCase imports;
    private final TranslationQueryUseCase queries;
    private final TranslationSpreadsheet spreadsheet;
    private final MessageResolver messages;
    private final StoredTranslationResolver resolverCache;
    private final CachedTranslationBundles bundles;

    @GetMapping("/export")
    @RequirePermission(TranslationPermission.Values.TRANSLATIONS_READ)
    public ResponseEntity<byte[]> export() {
        List<String> languages = queries.supportedLanguages();
        byte[] file = spreadsheet.write(headers(languages), languages, exports.exportRows());

        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(TranslationSpreadsheet.FILE_NAME).build().toString()
            )
            .contentType(MediaType.parseMediaType(TranslationSpreadsheet.CONTENT_TYPE))
            .body(file);
    }

    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(TranslationPermission.Values.TRANSLATIONS_EDIT)
    public ImportReport importFile(@AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file) {
        List<String> languages = queries.supportedLanguages();
        List<ImportedTranslation> rows = rowsOf(file, languages);

        ImportReport report = imports.importOverrides(new ImportTranslationsCommand(rows, JwtAuthor.of(jwt)));
        resolverCache.invalidate();
        bundles.invalidate();

        return report;
    }

    private List<String> headers(List<String> languages) {
        List<String> headers = new ArrayList<>(languages.size() + 2);
        headers.add(messages.resolve(KEY_COLUMN_LABEL));
        headers.add(messages.resolve(CONTEXT_COLUMN_LABEL));
        headers.addAll(languages);
        return headers;
    }

    private List<ImportedTranslation> rowsOf(MultipartFile file, List<String> languages) {
        try (InputStream input = file.getInputStream()) {
            return spreadsheet.read(input, languages);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

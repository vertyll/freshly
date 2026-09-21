package com.vertyll.freshly.translation.infrastructure.web.controller;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PagedResponse;
import com.vertyll.freshly.translation.application.dto.TranslationBundle;
import com.vertyll.freshly.translation.application.dto.TranslationEntry;
import com.vertyll.freshly.translation.application.port.inbound.command.TranslationAdminUseCase;
import com.vertyll.freshly.translation.application.port.inbound.query.TranslationQueryUseCase;
import com.vertyll.freshly.translation.application.security.TranslationPermission;
import com.vertyll.freshly.translation.domain.model.SupportedLanguage;
import com.vertyll.freshly.translation.infrastructure.resolver.CachedTranslationBundles;
import com.vertyll.freshly.translation.infrastructure.resolver.StoredTranslationResolver;
import com.vertyll.freshly.translation.infrastructure.web.dto.OverrideTranslationRequestDto;
import com.vertyll.freshly.web.http.ETagUtil;
import com.vertyll.freshly.web.security.PublicEndpoint;
import com.vertyll.freshly.web.security.RequirePermission;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/translations")
@RequiredArgsConstructor
public class TranslationController {
    private static final String DEFAULT_PAGE = "0";
    private static final String DEFAULT_SIZE = "50";

    private static final Duration BUNDLE_MAX_AGE = Duration.ofMinutes(5);

    private final TranslationQueryUseCase queries;
    private final TranslationAdminUseCase admin;
    private final StoredTranslationResolver resolverCache;
    private final CachedTranslationBundles bundles;

    @GetMapping("/bundles/{language}")
    @PublicEndpoint("the sign-in page needs its labels before anyone can sign in")
    public ResponseEntity<Map<String, String>> bundle(
        @PathVariable String language,
        @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) @Nullable String ifNoneMatch
    ) {
        TranslationBundle bundle = bundles.bundleFor(SupportedLanguage.require(language).tag());
        String etag = "\"" + bundle.etag() + "\"";

        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }

        return ResponseEntity.ok()
            .eTag(etag)
            .cacheControl(CacheControl.maxAge(BUNDLE_MAX_AGE).mustRevalidate())
            .body(bundle.messages());
    }

    @GetMapping("/languages")
    @PublicEndpoint("a client needs the language list to offer a switcher")
    public List<String> supportedLanguages() {
        return queries.supportedLanguages();
    }

    @GetMapping
    @RequirePermission(TranslationPermission.Values.TRANSLATIONS_READ)
    public PagedResponse<TranslationEntry> list(
        @RequestParam(required = false) @Nullable String search,
        @RequestParam(defaultValue = DEFAULT_PAGE) int page,
        @RequestParam(defaultValue = DEFAULT_SIZE) int size
    ) {
        PageRequest pageRequest = new PageRequest(page, size);

        return search == null || search.isBlank() ? queries.list(pageRequest) : queries.search(search, pageRequest);
    }

    @GetMapping("/stale")
    @RequirePermission(TranslationPermission.Values.TRANSLATIONS_READ)
    public List<TranslationEntry> staleOverrides() {
        return queries.staleOverrides();
    }

    @GetMapping("/orphans")
    @RequirePermission(TranslationPermission.Values.TRANSLATIONS_READ)
    public List<TranslationEntry> orphans() {
        return queries.orphans();
    }

    @PutMapping("/{key}/{language}")
    @RequirePermission(TranslationPermission.Values.TRANSLATIONS_EDIT)
    public ResponseEntity<TranslationEntry> override(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String key,
        @PathVariable String language,
        @Valid @RequestBody OverrideTranslationRequestDto request,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) @Nullable String ifMatch
    ) {
        TranslationEntry updated =
                admin.override(request.toCommand(key, language, JwtAuthor.of(jwt), ETagUtil.parseVersion(ifMatch)));
        resolverCache.invalidate();
        bundles.invalidate();

        return ResponseEntity.ok().eTag(ETagUtil.buildWeakETag(updated.version())).body(updated);
    }

    @DeleteMapping("/{key}/{language}")
    @RequirePermission(TranslationPermission.Values.TRANSLATIONS_EDIT)
    public ResponseEntity<TranslationEntry> clearOverride(@PathVariable String key, @PathVariable String language) {
        TranslationEntry updated = admin.clearOverride(key, language);
        resolverCache.invalidate();
        bundles.invalidate();

        return ResponseEntity.ok().eTag(ETagUtil.buildWeakETag(updated.version())).body(updated);
    }
}

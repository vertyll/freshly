package com.vertyll.freshly.auth.infrastructure.web.session;

import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

public final class FetchMetadata {
    private static final String FETCH_SITE_HEADER = "Sec-Fetch-Site";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final Set<String> TRUSTED_FETCH_SITES = Set.of("same-origin", "none");

    private FetchMetadata() {
    }

    public static boolean sentFromThisOrigin(HttpServletRequest request) {
        if (SAFE_METHODS.contains(request.getMethod())) {
            return true;
        }
        String fetchSite = request.getHeader(FETCH_SITE_HEADER);
        return fetchSite == null || TRUSTED_FETCH_SITES.contains(fetchSite);
    }
}

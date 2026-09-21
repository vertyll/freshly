package com.vertyll.freshly.lang.page;

/**
 * A request for a page of results, expressed without Spring Data.
 *
 * <p>
 * A use case takes this and returns a {@link PageResult}; the conversion to and
 * from {@code org.springframework.data.domain.Pageable} happens in the web
 * adapter. Accepting Spring's {@code Pageable} inside the hexagon would put the
 * whole framework on the application layer's classpath in order to express two
 * integers.
 */
public record PageRequest(int page, int size) {

    private static final int MAX_SIZE = 200;

    private static final String PAGE_NEGATIVE = "Page index must not be negative";
    private static final String SIZE_NOT_POSITIVE = "Page size must be positive";
    private static final String SIZE_TOO_LARGE = "Page size must not exceed " + MAX_SIZE;

    public PageRequest {
        if (page < 0) {
            throw new IllegalArgumentException(PAGE_NEGATIVE);
        }
        if (size <= 0) {
            throw new IllegalArgumentException(SIZE_NOT_POSITIVE);
        }
        if (size > MAX_SIZE) {
            throw new IllegalArgumentException(SIZE_TOO_LARGE);
        }
    }
}

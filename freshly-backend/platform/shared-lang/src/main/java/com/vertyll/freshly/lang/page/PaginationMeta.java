package com.vertyll.freshly.lang.page;

/**
 * Where a page sits in the whole result, nested under its own member rather than flattened
 * beside the items.
 *
 * <p>
 * The nesting is the point. Flattened, a response mixes the resources with facts about
 * the query, and a client reading the body has to know which top-level keys are which. With
 * {@code items} and {@code pagination} as siblings, that question does not arise, and a
 * metadata field added later cannot collide with anything.
 *
 * @param hasMore whether asking for the next page would return anything
 */
public record PaginationMeta(long total, int page, int pageSize, int totalPages, boolean hasMore) {

    public static PaginationMeta of(PageRequest request, long total) {
        return of(request.page(), request.size(), total);
    }

    /**
     * The one place {@code totalPages} and {@code hasMore} are derived.
     *
     * <p>
     * {@link PagedResponse} builds its metadata from a {@code PageResult} rather than a
     * {@code PageRequest}, and restating the arithmetic there would mean two definitions
     * of "is there a next page" that the compiler cannot hold together.
     */
    public static PaginationMeta of(int page, int size, long total) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceilDiv(total, size);
        return new PaginationMeta(total, page, size, totalPages, page + 1 < totalPages);
    }
}

package com.vertyll.freshly.lang.page;

import java.util.List;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * One page as a <em>repository</em> hands it back.
 *
 * <p>
 * Paired with {@link PagedResponse}, which is what a query use case returns. The split
 * is deliberate: this one holds domain models and has whatever shape paging needs, that
 * one holds DTOs and has the shape the API promises. Collapsing them would make the
 * storage adapter's convenience into the client's contract.
 *
 * <p>
 * So this type should not appear above a repository port. {@code PagedResponse.from}
 * is the crossing point, and the only place both are named.
 *
 * @param <T> a domain model
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {
    private static final String CONTENT_CANNOT_BE_NULL = "Content cannot be null";
    private static final String PAGE_NEGATIVE = "Page index must not be negative";
    private static final String SIZE_NOT_POSITIVE = "Page size must be positive";
    private static final String TOTAL_NEGATIVE = "Total elements must not be negative";

    public PageResult {
        content = List.copyOf(requireNonNull(content, CONTENT_CANNOT_BE_NULL));

        if (page < 0) {
            throw new IllegalArgumentException(PAGE_NEGATIVE);
        }
        if (size <= 0) {
            throw new IllegalArgumentException(SIZE_NOT_POSITIVE);
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException(TOTAL_NEGATIVE);
        }
    }

    public static <T> PageResult<T> empty(PageRequest request) {
        return new PageResult<>(List.of(), request.page(), request.size(), 0L);
    }

    public int totalPages() {
        return (int) Math.ceilDiv(totalElements, size);
    }

    /**
     * Maps the content while keeping the paging metadata, so an adapter can turn a
     * page of domain models into a page of DTOs without restating the counts.
     */
    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PageResult<>(content.stream().<R>map(mapper).toList(), page, size, totalElements);
    }
}

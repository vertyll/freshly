package com.vertyll.freshly.lang.page;

import java.util.List;
import java.util.function.Function;

/**
 * A page of results as an <em>answer</em>, distinct from {@link PageResult}, which is what
 * a repository hands back.
 *
 * <p>
 * Two page types at two layers is deliberate. They are not the same thing:
 *
 * <table>
 * <caption>Which page type belongs where</caption>
 * <tr>
 * <th></th>
 * <th>{@link PageResult}</th>
 * <th>{@code PagedResponse}</th>
 * </tr>
 * <tr>
 * <td>Spoken by</td>
 * <td>repository ports</td>
 * <td>query use cases</td>
 * </tr>
 * <tr>
 * <td>Holds</td>
 * <td>domain models</td>
 * <td>response DTOs</td>
 * </tr>
 * <tr>
 * <td>Shape</td>
 * <td>flat, whatever paging needs</td>
 * <td>the API's contract</td>
 * </tr>
 * </table>
 *
 * <p>
 * Collapsing them would mean the repository's convenience shape is also the shape every
 * client sees, so adding a field a query needs becomes an API change.
 */
public record PagedResponse<T>(List<T> items, PaginationMeta pagination) {

    public PagedResponse {
        items = List.copyOf(items);
    }

    public static <T> PagedResponse<T> of(List<T> items, PageRequest request, long total) {
        return new PagedResponse<>(items, PaginationMeta.of(request, total));
    }

    public static <T> PagedResponse<T> from(PageResult<T> result) {
        return new PagedResponse<>(
            result.content(),
            PaginationMeta.of(result.page(), result.size(), result.totalElements())
        );
    }

    public static <T> PagedResponse<T> empty(PageRequest request) {
        return of(List.of(), request, 0L);
    }

    /**
     * Maps the contents while keeping the metadata.
     *
     * <p>
     * What a query service uses to turn a page of domain models into a page of DTOs
     * without restating the counts, and what a controller uses when its wire DTO differs
     * from the application's.
     */
    public <R> PagedResponse<R> map(Function<? super T, ? extends R> mapper) {
        return new PagedResponse<>(items.stream().<R>map(mapper).toList(), pagination);
    }
}

package com.vertyll.freshly.lang.page;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagingTest {
    @Test
    void aRequestForANegativePageIsRefused() {
        assertThatThrownBy(() -> new PageRequest(-1, 10)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aRequestForAnEmptyPageIsRefused() {
        assertThatThrownBy(() -> new PageRequest(0, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aPageOfTwoHundredIsTheLargestThatCanBeAskedFor() {
        assertThat(new PageRequest(0, 200).size()).isEqualTo(200);
        assertThatThrownBy(() -> new PageRequest(0, 201)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aResultCannotHaveANegativePageOrTotalOrNoSize() {
        assertThatThrownBy(() -> new PageResult<>(List.of(), -1, 10, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageResult<>(List.of(), 0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageResult<>(List.of(), 0, 10, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aResultDoesNotFollowTheListItCameFrom() {
        List<String> content = new ArrayList<>(List.of("a"));
        PageResult<String> result = new PageResult<>(content, 0, 10, 1);

        content.add("b");

        assertThat(result.content()).containsExactly("a");
    }

    @Test
    void anEmptyResultKeepsThePageItWasAskedFor() {
        PageResult<String> result = PageResult.empty(new PageRequest(3, 20));

        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(3);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void aPartlyFilledLastPageCountsAsAPage() {
        assertThat(new PageResult<>(List.of("a"), 0, 10, 21).totalPages()).isEqualTo(3);
    }

    @Test
    void mappingAResultKeepsItsCounts() {
        PageResult<Integer> mapped = new PageResult<>(List.of("abc"), 2, 10, 21).map(String::length);

        assertThat(mapped.content()).containsExactly(3);
        assertThat(mapped.page()).isEqualTo(2);
        assertThat(mapped.totalElements()).isEqualTo(21);
    }

    @Test
    void thereIsMoreUntilTheLastPage() {
        assertThat(PaginationMeta.of(0, 10, 11).hasMore()).isTrue();
        assertThat(PaginationMeta.of(1, 10, 11).hasMore()).isFalse();
    }

    @Test
    void noElementsMeanNoPages() {
        PaginationMeta meta = PaginationMeta.of(new PageRequest(0, 10), 0);

        assertThat(meta.totalPages()).isZero();
        assertThat(meta.hasMore()).isFalse();
    }

    @Test
    void aSizeOfZeroInTheMetadataMeansNoPages() {
        assertThat(PaginationMeta.of(0, 0, 5).totalPages()).isZero();
    }

    @Test
    void aResponseCarriesTheCountsOfItsResult() {
        PagedResponse<String> response = PagedResponse.from(new PageResult<>(List.of("a"), 1, 2, 3));

        assertThat(response.items()).containsExactly("a");
        assertThat(response.pagination()).isEqualTo(new PaginationMeta(3, 1, 2, 2, false));
    }

    @Test
    void aResponseCanBeBuiltFromTheRequest() {
        PagedResponse<String> response = PagedResponse.of(List.of("a"), new PageRequest(0, 2), 3);

        assertThat(response.pagination()).isEqualTo(new PaginationMeta(3, 0, 2, 2, true));
    }

    @Test
    void anEmptyResponseHasNoPages() {
        PagedResponse<String> response = PagedResponse.empty(new PageRequest(0, 10));

        assertThat(response.items()).isEmpty();
        assertThat(response.pagination().total()).isZero();
    }

    @Test
    void mappingAResponseKeepsItsMetadata() {
        PagedResponse<Integer> mapped = PagedResponse.of(List.of("abc"), new PageRequest(0, 2), 3).map(String::length);

        assertThat(mapped.items()).containsExactly(3);
        assertThat(mapped.pagination().total()).isEqualTo(3);
    }
}

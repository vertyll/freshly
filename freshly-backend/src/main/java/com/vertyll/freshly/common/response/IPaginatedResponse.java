package com.vertyll.freshly.common.response;

public interface IPaginatedResponse<T> {
    Integer getPageNumber();

    Integer getPageSize();

    Long getTotalElements();

    Integer getTotalPages();

    Boolean isFirst();

    Boolean isLast();

    Boolean isEmpty();

    default Boolean hasNext() {
        return isLast() != null && !isLast();
    }

    default Boolean hasPrevious() {
        return isFirst() != null && !isFirst();
    }
}

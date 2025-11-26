package com.vertyll.freshly.common.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
public abstract class BaseResponse<T> implements IResponse<T> {
    protected T data;
    protected String message;

    @Builder.Default
    protected LocalDateTime timestamp = LocalDateTime.now();
}

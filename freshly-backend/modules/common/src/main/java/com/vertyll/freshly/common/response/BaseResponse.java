package com.vertyll.freshly.common.response;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.jspecify.annotations.Nullable;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
public abstract class BaseResponse<T> implements IResponse<T> {
    @Nullable protected T data;
    @Nullable protected String message;

    @Builder.Default protected LocalDateTime timestamp = LocalDateTime.now(ZoneOffset.UTC);
}

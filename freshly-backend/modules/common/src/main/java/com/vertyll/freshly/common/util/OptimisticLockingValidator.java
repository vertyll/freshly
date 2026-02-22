package com.vertyll.freshly.common.util;

import java.util.Objects;

import org.springframework.dao.OptimisticLockingFailureException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OptimisticLockingValidator {

    public static void validate(Long currentVersion, Long expectedVersion) {
        if (expectedVersion != null && !Objects.equals(currentVersion, expectedVersion)) {
            throw new OptimisticLockingFailureException(
                    String.format(
                            "Version mismatch: expected %d but was %d",
                            expectedVersion, currentVersion));
        }
    }
}

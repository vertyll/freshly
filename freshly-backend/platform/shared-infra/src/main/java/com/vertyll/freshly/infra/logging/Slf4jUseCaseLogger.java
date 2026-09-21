package com.vertyll.freshly.infra.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vertyll.freshly.lang.logging.UseCaseLogger;

/**
 * The infrastructure side of the {@code UseCaseLogger} port.
 *
 * <p>
 * Takes the use-case class so the logger is named after the class a reader expects to
 * see in the output, not after this adapter. Without that, every line the application
 * layer writes would be attributed here and filtering logs by package would stop working.
 */
public class Slf4jUseCaseLogger implements UseCaseLogger {

    private final Logger delegate;

    public Slf4jUseCaseLogger(Class<?> useCase) {
        this.delegate = LoggerFactory.getLogger(useCase);
    }

    @Override
    public void debug(String message, Object... args) {
        delegate.debug(message, args);
    }

    @Override
    public void info(String message, Object... args) {
        delegate.info(message, args);
    }

    @Override
    public void warn(String message, Object... args) {
        delegate.warn(message, args);
    }

    @Override
    public void error(String message, Object... args) {
        delegate.error(message, args);
    }
}

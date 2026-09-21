package com.vertyll.freshly.lang.logging;

/**
 * Logging as an outbound port.
 *
 * <p>
 * Arguable — SLF4J is a facade rather than a framework — but admitting it as an
 * exception makes the dependency rule a sentence with a footnote, and a build
 * check cannot enforce a footnote. With this port the rule reads "the inside
 * depends on nothing external", and {@code checkHexagonalDependencies} verifies
 * exactly that.
 *
 * <p>
 * The infrastructure layer supplies {@code Slf4jUseCaseLogger}. A unit test
 * supplies a recording double, which also turns "did this use case log the
 * refusal" into an assertable fact rather than something you read in a console.
 */
public interface UseCaseLogger {

    void debug(String message, Object... args);

    void info(String message, Object... args);

    void warn(String message, Object... args);

    void error(String message, Object... args);
}

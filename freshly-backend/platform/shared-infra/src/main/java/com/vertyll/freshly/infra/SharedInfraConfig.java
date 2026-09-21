package com.vertyll.freshly.infra;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the shared infrastructure beans, imported once by {@code bootstrap}.
 *
 * <p>
 * Currently just {@code TransactionalUseCaseFactory}. {@code Slf4jUseCaseLogger} is
 * not a bean — it is constructed per use case in each module's
 * {@code ApplicationBeansConfig}, because it needs the use-case class to name the logger.
 */
@Configuration
@ComponentScan(basePackages = "com.vertyll.freshly.infra")
public class SharedInfraConfig {
}

/**
 * The use-case layer of a bounded context.
 *
 * Contains inbound ports (`...CommandUseCase`, `...QueryUseCase`), outbound ports,
 * commands, response DTOs and the services that implement the inbound ports.
 * It orchestrates the domain; it does not know what drives it or what backs it.
 *
 * Framework-free, which has consequences the infrastructure layer has to absorb:
 *
 * | Not available here | Where it lives instead                           |
 * |--------------------|--------------------------------------------------|
 * | `@Service`         | explicit `@Bean` in `ApplicationBeansConfig`     |
 * | `@Transactional`   | `TransactionalUseCaseFactory` proxy at the port  |
 * | SLF4J / `@Slf4j`   | the `UseCaseLogger` port                         |
 * | `@Valid` DTOs      | request DTOs in `infrastructure.web.dto`         |
 * | Spring `Pageable`  | `PageRequest` / `PageResult` from `shared-lang`  |
 *
 * The payoff is that every use case is constructible in a plain unit test with
 * no container, which is the property the whole arrangement exists to buy.
 */

plugins {
    id("freshly.java-conventions")
    `java-library`
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    api(project(":platform:shared-lang"))
    api(project(":platform:shared-authz"))

    testImplementation(libs.findBundle("test-unit").get())
}

HexagonalClasspathCheck.register(project, "application layer")

package com.vertyll.freshly.permission.infrastructure.config;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vertyll.freshly.authz.PermissionCatalogue;
import com.vertyll.freshly.authz.StockRole;
import com.vertyll.freshly.permission.application.port.inbound.command.RoleAuthorityCommandUseCase;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class PermissionSeeder {

    @Bean
    ApplicationRunner seedStockRoles(RoleAuthorityCommandUseCase roles, List<PermissionCatalogue> catalogues) {
        return _ -> {
            catalogues.forEach(PermissionSeeder::requireOwnPermissions);

            Set<StockRole> stockRoles = catalogues.stream()
                .flatMap(catalogue -> catalogue.stockRoles().stream())
                .collect(Collectors.toUnmodifiableSet());

            int applied = roles.seedStockRoles(stockRoles);
            log.info("Stock roles: {} applied, {} declared", applied, stockRoles.size());
        };
    }

    private static void requireOwnPermissions(PermissionCatalogue catalogue) {
        Set<String> declared = catalogue.values();

        catalogue.stockRoles().forEach(stock -> {
            Set<String> unknown = stock.permissions()
                .stream()
                .filter(permission -> !declared.contains(permission))
                .collect(Collectors.toCollection(TreeSet::new));

            if (!unknown.isEmpty()) {
                throw new IllegalStateException(
                    "Stock role " + stock.role() + " of " + catalogue.context()
                            + " grants permissions that module does not declare: " + unknown
                );
            }
        });
    }
}

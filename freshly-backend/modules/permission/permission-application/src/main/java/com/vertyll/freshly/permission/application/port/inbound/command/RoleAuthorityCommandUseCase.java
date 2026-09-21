package com.vertyll.freshly.permission.application.port.inbound.command;

import java.util.Collection;

import com.vertyll.freshly.authz.StockRole;
import com.vertyll.freshly.permission.application.command.ReplaceRoleAuthorityCommand;
import com.vertyll.freshly.permission.application.dto.RoleAuthorityResponse;

public interface RoleAuthorityCommandUseCase {
    RoleAuthorityResponse replace(ReplaceRoleAuthorityCommand command);

    void delete(String role);

    int seedStockRoles(Collection<StockRole> stockRoles);
}

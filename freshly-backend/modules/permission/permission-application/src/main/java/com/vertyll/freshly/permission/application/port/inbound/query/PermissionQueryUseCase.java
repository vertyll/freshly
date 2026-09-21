package com.vertyll.freshly.permission.application.port.inbound.query;

import java.util.List;

import com.vertyll.freshly.authz.PermissionEvaluator;
import com.vertyll.freshly.permission.application.dto.PermissionModuleResponse;
import com.vertyll.freshly.permission.application.dto.RoleAuthorityResponse;

public interface PermissionQueryUseCase extends PermissionEvaluator {

    List<RoleAuthorityResponse> listRoles();

    RoleAuthorityResponse roleAuthority(String role);

    List<PermissionModuleResponse> declaredPermissions();
}

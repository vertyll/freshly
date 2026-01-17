package com.vertyll.freshly.useraccess.api.mapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.vertyll.freshly.common.enums.UserRoleEnum;
import com.vertyll.freshly.common.mapper.MapStructConfig;
import com.vertyll.freshly.useraccess.api.dto.UserResponseDto;
import com.vertyll.freshly.useraccess.domain.SystemUser;

@Mapper(config = MapStructConfig.class)
public interface UserDtoMapper {

    @Mapping(target = "id", source = "keycloakUserId")
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "roles", source = "roles")
    UserResponseDto toResponse(SystemUser user);

    List<UserResponseDto> toResponseList(List<SystemUser> users);

    default Set<String> mapRolesToStrings(Set<UserRoleEnum> roles) {
        return roles.stream().map(UserRoleEnum::getValue).collect(Collectors.toSet());
    }
}

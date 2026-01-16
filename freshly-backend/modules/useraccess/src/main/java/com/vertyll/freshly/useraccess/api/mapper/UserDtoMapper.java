package com.vertyll.freshly.useraccess.api.mapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.vertyll.freshly.common.mapper.MapStructConfig;
import com.vertyll.freshly.useraccess.api.dto.UserResponseDto;
import com.vertyll.freshly.useraccess.domain.SystemUser;
import com.vertyll.freshly.useraccess.domain.UserRoleEnum;

@Mapper(config = MapStructConfig.class)
public interface UserDtoMapper {

    @Mapping(target = "id", source = "keycloakUserId")
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "roles", expression = "java(mapRolesToStrings(user.getRoles()))")
    @Mapping(target = "version", source = "version")
    UserResponseDto toResponse(SystemUser user);

    List<UserResponseDto> toResponseList(List<SystemUser> users);

    default Set<String> mapRolesToStrings(Set<UserRoleEnum> roles) {
        return roles.stream().map(Enum::name).collect(Collectors.toSet());
    }
}

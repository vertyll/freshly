package com.vertyll.freshly.useraccess.api.mapper;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.vertyll.freshly.useraccess.api.dto.UserResponseDto;
import com.vertyll.freshly.useraccess.domain.SystemUser;
import com.vertyll.freshly.useraccess.domain.UserRoleEnum;

class UserDtoMapperTest {

    private UserDtoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(UserDtoMapper.class);
    }

    @Test
    @DisplayName("Should map SystemUser to UserResponseDto")
    void shouldMapSystemUserToUserResponseDto() {
        // Given
        UUID userId = UUID.randomUUID();
        SystemUser user = new SystemUser(userId, true, Set.of(UserRoleEnum.USER));

        // When
        UserResponseDto result = mapper.toResponse(user);

        // Then
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.isActive()).isTrue();
        assertThat(result.roles()).containsExactly("USER");
    }

    @Test
    @DisplayName("Should map inactive user to UserResponseDto")
    void shouldMapInactiveUserToUserResponseDto() {
        // Given
        UUID userId = UUID.randomUUID();
        SystemUser user = new SystemUser(userId, false, Set.of(UserRoleEnum.USER));

        // When
        UserResponseDto result = mapper.toResponse(user);

        // Then
        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should map user with multiple roles to UserResponseDto")
    void shouldMapUserWithMultipleRolesToUserResponseDto() {
        // Given
        UUID userId = UUID.randomUUID();
        SystemUser user =
                new SystemUser(userId, true, Set.of(UserRoleEnum.USER, UserRoleEnum.ADMIN));

        // When
        UserResponseDto result = mapper.toResponse(user);

        // Then
        assertThat(result.roles()).containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    @DisplayName("Should map admin user to UserResponseDto")
    void shouldMapAdminUserToUserResponseDto() {
        // Given
        UUID userId = UUID.randomUUID();
        SystemUser user = new SystemUser(userId, true, Set.of(UserRoleEnum.ADMIN));

        // When
        UserResponseDto result = mapper.toResponse(user);

        // Then
        assertThat(result.roles()).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("Should map list of users to list of UserResponseDto")
    void shouldMapListOfUsersToListOfUserResponseDto() {
        // Given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        SystemUser user1 = new SystemUser(userId1, true, Set.of(UserRoleEnum.USER));
        SystemUser user2 = new SystemUser(userId2, false, Set.of(UserRoleEnum.ADMIN));
        List<SystemUser> users = List.of(user1, user2);

        // When
        List<UserResponseDto> result = mapper.toResponseList(users);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.getFirst().id()).isEqualTo(userId1);
        assertThat(result.get(0).isActive()).isTrue();
        assertThat(result.get(0).roles()).containsExactly("USER");
        assertThat(result.get(1).id()).isEqualTo(userId2);
        assertThat(result.get(1).isActive()).isFalse();
        assertThat(result.get(1).roles()).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("Should map empty list to empty list")
    void shouldMapEmptyListToEmptyList() {
        // Given
        List<SystemUser> users = List.of();

        // When
        List<UserResponseDto> result = mapper.toResponseList(users);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should map roles to strings correctly")
    void shouldMapRolesToStringsCorrectly() {
        // Given
        Set<UserRoleEnum> roles = Set.of(UserRoleEnum.USER, UserRoleEnum.ADMIN);

        // When
        Set<String> result = mapper.mapRolesToStrings(roles);

        // Then
        assertThat(result).containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    @DisplayName("Should map single role to string correctly")
    void shouldMapSingleRoleToStringCorrectly() {
        // Given
        Set<UserRoleEnum> roles = Set.of(UserRoleEnum.USER);

        // When
        Set<String> result = mapper.mapRolesToStrings(roles);

        // Then
        assertThat(result).containsExactly("USER");
    }

    @Test
    @DisplayName("Should preserve user ID when mapping")
    void shouldPreserveUserIdWhenMapping() {
        // Given
        UUID specificUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        SystemUser user = new SystemUser(specificUserId, true, Set.of(UserRoleEnum.USER));

        // When
        UserResponseDto result = mapper.toResponse(user);

        // Then
        assertThat(result.id()).isEqualTo(specificUserId);
    }

    @Test
    @DisplayName("Should map multiple users with different states")
    void shouldMapMultipleUsersWithDifferentStates() {
        // Given
        SystemUser activeUser = new SystemUser(UUID.randomUUID(), true, Set.of(UserRoleEnum.USER));
        SystemUser inactiveAdmin =
                new SystemUser(UUID.randomUUID(), false, Set.of(UserRoleEnum.ADMIN));
        SystemUser activeMultiRole =
                new SystemUser(
                        UUID.randomUUID(), true, Set.of(UserRoleEnum.USER, UserRoleEnum.ADMIN));
        List<SystemUser> users = List.of(activeUser, inactiveAdmin, activeMultiRole);

        // When
        List<UserResponseDto> result = mapper.toResponseList(users);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).isActive()).isTrue();
        assertThat(result.get(0).roles()).containsExactly("USER");
        assertThat(result.get(1).isActive()).isFalse();
        assertThat(result.get(1).roles()).containsExactly("ADMIN");
        assertThat(result.get(2).isActive()).isTrue();
        assertThat(result.get(2).roles()).containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    @DisplayName("Should handle reconstituted user")
    void shouldHandleReconstituedUser() {
        // Given
        UUID userId = UUID.randomUUID();
        SystemUser user = SystemUser.reconstitute(userId, false, Set.of(UserRoleEnum.ADMIN), 1L);

        // When
        UserResponseDto result = mapper.toResponse(user);

        // Then
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.isActive()).isFalse();
        assertThat(result.roles()).containsExactly("ADMIN");
        assertThat(result.version()).isEqualTo(1L);
    }
}

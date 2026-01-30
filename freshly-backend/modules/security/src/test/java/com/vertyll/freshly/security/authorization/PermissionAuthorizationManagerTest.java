package com.vertyll.freshly.security.authorization;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;

import com.vertyll.freshly.common.annotation.RequirePermission;
import com.vertyll.freshly.common.enums.Permission;
import com.vertyll.freshly.permission.application.PermissionService;

@ExtendWith(MockitoExtension.class)
class PermissionAuthorizationManagerTest {
    private static final String METHOD_WITH_PERMISSION = "methodWithPermission";
    private static final String METHOD_WITHOUT_ANNOTATION = "methodWithoutAnnotation";
    private static final String METHOD_WITH_DIFFERENT_PERMISSION = "methodWithDifferentPermission";

    @Mock
    @SuppressWarnings("NullAway.Init")
    private PermissionService permissionService;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private MethodInvocation methodInvocation;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private Authentication authentication;

    private PermissionAuthorizationManager authorizationManager;

    @BeforeEach
    @SuppressWarnings("NullAway.Init")
    void setUp() {
        authorizationManager = new PermissionAuthorizationManager(permissionService);
    }

    @Test
    @DisplayName("Should grant access when user has required permission on method")
    void shouldGrantAccessWhenUserHasRequiredPermissionOnMethod() throws NoSuchMethodException {
        // Given
        Method method = SecurityMockTarget.class.getMethod(METHOD_WITH_PERMISSION);
        when(methodInvocation.getMethod()).thenReturn(method);
        when(permissionService.hasPermission(authentication, Permission.USERS_READ))
                .thenReturn(true);

        Supplier<Authentication> authSupplier = () -> authentication;

        // When
        AuthorizationResult result = authorizationManager.authorize(authSupplier, methodInvocation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isTrue();
        verify(permissionService).hasPermission(authentication, Permission.USERS_READ);
    }

    @Test
    @DisplayName("Should deny access when user does not have required permission on method")
    void shouldDenyAccessWhenUserDoesNotHaveRequiredPermissionOnMethod()
            throws NoSuchMethodException {
        // Given
        Method method = SecurityMockTarget.class.getMethod(METHOD_WITH_PERMISSION);
        when(methodInvocation.getMethod()).thenReturn(method);
        when(permissionService.hasPermission(authentication, Permission.USERS_READ))
                .thenReturn(false);

        Supplier<Authentication> authSupplier = () -> authentication;

        // When
        AuthorizationResult result = authorizationManager.authorize(authSupplier, methodInvocation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
        verify(permissionService).hasPermission(authentication, Permission.USERS_READ);
    }

    @Test
    @DisplayName("Should check class-level annotation when method annotation is absent")
    void shouldCheckClassLevelAnnotationWhenMethodAnnotationIsAbsent()
            throws NoSuchMethodException {
        // Given
        Method method = ClassWithPermission.class.getMethod(METHOD_WITHOUT_ANNOTATION);
        when(methodInvocation.getMethod()).thenReturn(method);
        when(permissionService.hasPermission(authentication, Permission.USERS_CREATE))
                .thenReturn(true);

        Supplier<Authentication> authSupplier = () -> authentication;

        // When
        AuthorizationResult result = authorizationManager.authorize(authSupplier, methodInvocation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isTrue();
        verify(permissionService).hasPermission(authentication, Permission.USERS_CREATE);
    }

    @Test
    @DisplayName("Should prioritize method-level annotation over class-level annotation")
    void shouldPrioritizeMethodLevelAnnotationOverClassLevelAnnotation()
            throws NoSuchMethodException {
        // Given
        Method method = ClassWithPermission.class.getMethod(METHOD_WITH_DIFFERENT_PERMISSION);
        when(methodInvocation.getMethod()).thenReturn(method);
        when(permissionService.hasPermission(authentication, Permission.USERS_DELETE))
                .thenReturn(true);

        Supplier<Authentication> authSupplier = () -> authentication;

        // When
        AuthorizationResult result = authorizationManager.authorize(authSupplier, methodInvocation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isTrue();
        verify(permissionService).hasPermission(authentication, Permission.USERS_DELETE);
        verify(permissionService, never()).hasPermission(authentication, Permission.USERS_CREATE);
    }

    @Test
    @DisplayName("Should deny access when no annotation is found")
    void shouldDenyAccessWhenNoAnnotationIsFound() throws NoSuchMethodException {
        // Given
        Method method = SecurityMockTarget.class.getMethod(METHOD_WITHOUT_ANNOTATION);
        when(methodInvocation.getMethod()).thenReturn(method);

        Supplier<Authentication> authSupplier = () -> authentication;

        // When
        AuthorizationResult result = authorizationManager.authorize(authSupplier, methodInvocation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
        verify(permissionService, never()).hasPermission(any(), any(Permission.class));
    }

    @Test
    @DisplayName("Should handle null authentication gracefully")
    @SuppressWarnings("NullAway")
    void shouldHandleNullAuthenticationGracefully() throws NoSuchMethodException {
        // Given
        Method method = SecurityMockTarget.class.getMethod(METHOD_WITH_PERMISSION);
        when(methodInvocation.getMethod()).thenReturn(method);
        when(permissionService.hasPermission(null, Permission.USERS_READ)).thenReturn(false);

        Supplier<Authentication> authSupplier = () -> null;

        // When
        AuthorizationResult result = authorizationManager.authorize(authSupplier, methodInvocation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
        verify(permissionService).hasPermission(null, Permission.USERS_READ);
    }

    // Test classes
    static class SecurityMockTarget {
        @RequirePermission(Permission.USERS_READ)
        @SuppressWarnings("unused")
        void methodWithPermission() {
            // Empty method used only for testing authorization annotations
        }

        @SuppressWarnings("unused")
        void methodWithoutAnnotation() {
            // Empty method used only for testing authorization behavior when annotation is absent
        }
    }

    @RequirePermission(Permission.USERS_CREATE)
    static class ClassWithPermission {
        @SuppressWarnings("unused")
        void methodWithoutAnnotation() {
            // Empty method used only for testing class-level authorization annotations
        }

        @RequirePermission(Permission.USERS_DELETE)
        @SuppressWarnings("unused")
        void methodWithDifferentPermission() {
            // Empty method used only for testing method-level vs class-level annotation priority
        }
    }
}

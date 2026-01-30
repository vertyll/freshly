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

import com.vertyll.freshly.common.annotation.RequireAnyPermission;
import com.vertyll.freshly.common.enums.Permission;
import com.vertyll.freshly.permission.application.PermissionService;

@ExtendWith(MockitoExtension.class)
class AnyPermissionAuthorizationManagerTest {
    private static final String METHOD_WITHOUT_ANNOTATION = "methodWithoutAnnotation";
    private static final String METHOD_WITH_DIFFERENT_PERMISSIONS =
            "methodWithDifferentPermissions";
    private static final String METHOD_WITH_MULTIPLE_PERMISSIONS = "methodWithMultiplePermissions";
    private static final String METHOD_WITH_SINGLE_PERMISSION = "methodWithSinglePermission";

    @Mock
    @SuppressWarnings("NullAway.Init")
    private PermissionService permissionService;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private MethodInvocation methodInvocation;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private Authentication authentication;

    private AnyPermissionAuthorizationManager authorizationManager;

    @BeforeEach
    @SuppressWarnings("NullAway.Init")
    void setUp() {
        authorizationManager = new AnyPermissionAuthorizationManager(permissionService);
    }

    @Test
    @DisplayName("Should grant access when user has any of required permissions on method")
    void shouldGrantAccessWhenUserHasAnyOfRequiredPermissionsOnMethod()
            throws NoSuchMethodException {
        // Given
        Method method = SecurityMockTarget.class.getMethod(METHOD_WITH_MULTIPLE_PERMISSIONS);
        when(methodInvocation.getMethod()).thenReturn(method);
        Permission[] permissions = {
            Permission.USERS_READ, Permission.USERS_CREATE, Permission.USERS_DELETE
        };
        when(permissionService.hasAnyPermission(authentication, permissions)).thenReturn(true);

        Supplier<Authentication> authSupplier = () -> authentication;

        // When
        AuthorizationResult result = authorizationManager.authorize(authSupplier, methodInvocation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isTrue();
        verify(permissionService).hasAnyPermission(authentication, permissions);
    }

    @Test
    @DisplayName("Should deny access when user does not have any of required permissions on method")
    void shouldDenyAccessWhenUserDoesNotHaveAnyOfRequiredPermissionsOnMethod()
            throws NoSuchMethodException {
        // Given
        Method method = SecurityMockTarget.class.getMethod("methodWithMultiplePermissions");
        when(methodInvocation.getMethod()).thenReturn(method);
        Permission[] permissions = {
            Permission.USERS_READ, Permission.USERS_CREATE, Permission.USERS_DELETE
        };
        when(permissionService.hasAnyPermission(authentication, permissions)).thenReturn(false);

        Supplier<Authentication> authSupplier = () -> authentication;

        // When
        AuthorizationResult result = authorizationManager.authorize(authSupplier, methodInvocation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
        verify(permissionService).hasAnyPermission(authentication, permissions);
    }

    @Test
    @DisplayName("Should check class-level annotation when method annotation is absent")
    void shouldCheckClassLevelAnnotationWhenMethodAnnotationIsAbsent()
            throws NoSuchMethodException {
        // Given
        Method method = ClassWithAnyPermission.class.getMethod(METHOD_WITHOUT_ANNOTATION);
        when(methodInvocation.getMethod()).thenReturn(method);
        Permission[] permissions = {Permission.USERS_READ, Permission.USERS_CREATE};
        when(permissionService.hasAnyPermission(authentication, permissions)).thenReturn(true);

        Supplier<Authentication> authSupplier = () -> authentication;

        // When
        AuthorizationResult result = authorizationManager.authorize(authSupplier, methodInvocation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isTrue();
        verify(permissionService).hasAnyPermission(authentication, permissions);
    }

    @Test
    @DisplayName("Should prioritize method-level annotation over class-level annotation")
    void shouldPrioritizeMethodLevelAnnotationOverClassLevelAnnotation()
            throws NoSuchMethodException {
        // Given
        Method method = ClassWithAnyPermission.class.getMethod(METHOD_WITH_DIFFERENT_PERMISSIONS);
        when(methodInvocation.getMethod()).thenReturn(method);
        Permission[] methodPermissions = {Permission.REPORTS_READ, Permission.REPORTS_GENERATE};
        when(permissionService.hasAnyPermission(authentication, methodPermissions))
                .thenReturn(true);

        Supplier<Authentication> authSupplier = () -> authentication;

        // When
        AuthorizationResult result = authorizationManager.authorize(authSupplier, methodInvocation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isTrue();
        verify(permissionService).hasAnyPermission(authentication, methodPermissions);
        verify(permissionService, never())
                .hasAnyPermission(authentication, Permission.USERS_READ, Permission.USERS_CREATE);
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
        verify(permissionService, never()).hasAnyPermission(any(), any(Permission[].class));
    }

    @Test
    @DisplayName("Should handle single permission in annotation")
    void shouldHandleSinglePermissionInAnnotation() throws NoSuchMethodException {
        // Given
        Method method = SecurityMockTarget.class.getMethod(METHOD_WITH_SINGLE_PERMISSION);
        when(methodInvocation.getMethod()).thenReturn(method);
        Permission[] permissions = {Permission.USERS_UPDATE};
        when(permissionService.hasAnyPermission(authentication, permissions)).thenReturn(true);

        Supplier<Authentication> authSupplier = () -> authentication;

        // When
        AuthorizationResult result = authorizationManager.authorize(authSupplier, methodInvocation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isTrue();
        verify(permissionService).hasAnyPermission(authentication, permissions);
    }

    @Test
    @DisplayName("Should handle null authentication gracefully")
    @SuppressWarnings("NullAway")
    void shouldHandleNullAuthenticationGracefully() throws NoSuchMethodException {
        // Given
        Method method = SecurityMockTarget.class.getMethod(METHOD_WITH_MULTIPLE_PERMISSIONS);
        when(methodInvocation.getMethod()).thenReturn(method);
        Permission[] permissions = {
            Permission.USERS_READ, Permission.USERS_CREATE, Permission.USERS_DELETE
        };
        when(permissionService.hasAnyPermission(null, permissions)).thenReturn(false);

        Supplier<Authentication> authSupplier = () -> null;

        // When
        AuthorizationResult result = authorizationManager.authorize(authSupplier, methodInvocation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
        verify(permissionService).hasAnyPermission(null, permissions);
    }

    // Test classes
    static class SecurityMockTarget {
        @RequireAnyPermission({
            Permission.USERS_READ,
            Permission.USERS_CREATE,
            Permission.USERS_DELETE
        })
        void methodWithMultiplePermissions() {
            // Empty method used only for testing authorization annotations
        }

        @RequireAnyPermission(Permission.USERS_UPDATE)
        @SuppressWarnings("unused")
        void methodWithSinglePermission() {
            // Empty method used only for testing authorization annotations
        }

        @SuppressWarnings("unused")
        void methodWithoutAnnotation() {
            // Empty method used only for testing authorization behavior when annotation is absent
        }
    }

    @RequireAnyPermission({Permission.USERS_READ, Permission.USERS_CREATE})
    static class ClassWithAnyPermission {
        @SuppressWarnings("unused")
        void methodWithoutAnnotation() {
            // Empty method used only for testing class-level authorization annotations
        }

        @RequireAnyPermission({Permission.REPORTS_READ, Permission.REPORTS_GENERATE})
        @SuppressWarnings("unused")
        void methodWithDifferentPermissions() {
            // Empty method used only for testing method-level vs class-level annotation priority
        }
    }
}

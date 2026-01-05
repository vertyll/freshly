package com.vertyll.freshly.security.authorization;

import com.vertyll.freshly.permission.application.PermissionService;
import com.vertyll.freshly.security.annotation.RequireAnyPermission;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnyPermissionAuthorizationManagerTest {

    @Mock
    private PermissionService permissionService;

    @Mock
    private MethodInvocation methodInvocation;

    @Mock
    private Authentication authentication;

    private AnyPermissionAuthorizationManager authorizationManager;

    @BeforeEach
    void setUp() {
        authorizationManager = new AnyPermissionAuthorizationManager(permissionService);
    }

    @Test
    @DisplayName("Should grant access when user has any of required permissions on method")
    void shouldGrantAccessWhenUserHasAnyOfRequiredPermissionsOnMethod() throws NoSuchMethodException {
        // Given
        Method method = SecurityMockTarget.class.getMethod("methodWithMultiplePermissions");
        when(methodInvocation.getMethod()).thenReturn(method);
        String[] permissions = {"READ_DATA", "WRITE_DATA", "DELETE_DATA"};
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
    void shouldDenyAccessWhenUserDoesNotHaveAnyOfRequiredPermissionsOnMethod() throws NoSuchMethodException {
        // Given
        Method method = SecurityMockTarget.class.getMethod("methodWithMultiplePermissions");
        when(methodInvocation.getMethod()).thenReturn(method);
        String[] permissions = {"READ_DATA", "WRITE_DATA", "DELETE_DATA"};
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
    void shouldCheckClassLevelAnnotationWhenMethodAnnotationIsAbsent() throws NoSuchMethodException {
        // Given
        Method method = ClassWithAnyPermission.class.getMethod("methodWithoutAnnotation");
        when(methodInvocation.getMethod()).thenReturn(method);
        String[] permissions = {"ADMIN", "MODERATOR"};
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
    void shouldPrioritizeMethodLevelAnnotationOverClassLevelAnnotation() throws NoSuchMethodException {
        // Given
        Method method = ClassWithAnyPermission.class.getMethod("methodWithDifferentPermissions");
        when(methodInvocation.getMethod()).thenReturn(method);
        String[] methodPermissions = {"USER", "GUEST"};
        when(permissionService.hasAnyPermission(authentication, methodPermissions)).thenReturn(true);

        Supplier<Authentication> authSupplier = () -> authentication;

        // When
        AuthorizationResult result = authorizationManager.authorize(authSupplier, methodInvocation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isTrue();
        verify(permissionService).hasAnyPermission(authentication, methodPermissions);
        verify(permissionService, never()).hasAnyPermission(authentication, "ADMIN", "MODERATOR");
    }

    @Test
    @DisplayName("Should deny access when no annotation is found")
    void shouldDenyAccessWhenNoAnnotationIsFound() throws NoSuchMethodException {
        // Given
        Method method = SecurityMockTarget.class.getMethod("methodWithoutAnnotation");
        when(methodInvocation.getMethod()).thenReturn(method);

        Supplier<Authentication> authSupplier = () -> authentication;

        // When
        AuthorizationResult result = authorizationManager.authorize(authSupplier, methodInvocation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
        verify(permissionService, never()).hasAnyPermission(any(), any(String[].class));
    }

    @Test
    @DisplayName("Should handle single permission in annotation")
    void shouldHandleSinglePermissionInAnnotation() throws NoSuchMethodException {
        // Given
        Method method = SecurityMockTarget.class.getMethod("methodWithSinglePermission");
        when(methodInvocation.getMethod()).thenReturn(method);
        String[] permissions = {"SINGLE_PERMISSION"};
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
    void shouldHandleNullAuthenticationGracefully() throws NoSuchMethodException {
        // Given
        Method method = SecurityMockTarget.class.getMethod("methodWithMultiplePermissions");
        when(methodInvocation.getMethod()).thenReturn(method);
        String[] permissions = {"READ_DATA", "WRITE_DATA", "DELETE_DATA"};
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
    public static class SecurityMockTarget {
        @RequireAnyPermission({"READ_DATA", "WRITE_DATA", "DELETE_DATA"})
        public void methodWithMultiplePermissions() {
            // Empty method used only for testing authorization annotations
        }

        @RequireAnyPermission("SINGLE_PERMISSION")
        public void methodWithSinglePermission() {
            // Empty method used only for testing authorization annotations
        }

        public void methodWithoutAnnotation() {
            // Empty method used only for testing authorization behavior when annotation is absent
        }
    }

    @RequireAnyPermission({"ADMIN", "MODERATOR"})
    public static class ClassWithAnyPermission {
        public void methodWithoutAnnotation() {
            // Empty method used only for testing class-level authorization annotations
        }

        @RequireAnyPermission({"USER", "GUEST"})
        public void methodWithDifferentPermissions() {
            // Empty method used only for testing method-level vs class-level annotation priority
        }
    }
}

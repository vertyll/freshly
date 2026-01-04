package com.vertyll.freshly.security.authorization;

import com.vertyll.freshly.permission.application.PermissionService;
import com.vertyll.freshly.security.annotation.RequirePermission;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionAuthorizationManagerTest {

    @Mock
    private PermissionService permissionService;

    @Mock
    private MethodInvocation methodInvocation;

    @Mock
    private Authentication authentication;

    private PermissionAuthorizationManager authorizationManager;

    @BeforeEach
    void setUp() {
        authorizationManager = new PermissionAuthorizationManager(permissionService);
    }

    @Test
    @DisplayName("Should grant access when user has required permission on method")
    void shouldGrantAccessWhenUserHasRequiredPermissionOnMethod() throws NoSuchMethodException {
        // Given
        Method method = TestClass.class.getMethod("methodWithPermission");
        when(methodInvocation.getMethod()).thenReturn(method);
        when(permissionService.hasPermission(authentication, "READ_DATA")).thenReturn(true);

        Supplier<Authentication> authSupplier = () -> authentication;

        // When
        AuthorizationResult result = authorizationManager.authorize(authSupplier, methodInvocation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isTrue();
        verify(permissionService).hasPermission(authentication, "READ_DATA");
    }

    @Test
    @DisplayName("Should deny access when user does not have required permission on method")
    void shouldDenyAccessWhenUserDoesNotHaveRequiredPermissionOnMethod() throws NoSuchMethodException {
        // Given
        Method method = TestClass.class.getMethod("methodWithPermission");
        when(methodInvocation.getMethod()).thenReturn(method);
        when(permissionService.hasPermission(authentication, "READ_DATA")).thenReturn(false);

        Supplier<Authentication> authSupplier = () -> authentication;

        // When
        AuthorizationResult result = authorizationManager.authorize(authSupplier, methodInvocation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
        verify(permissionService).hasPermission(authentication, "READ_DATA");
    }

    @Test
    @DisplayName("Should check class-level annotation when method annotation is absent")
    void shouldCheckClassLevelAnnotationWhenMethodAnnotationIsAbsent() throws NoSuchMethodException {
        // Given
        Method method = ClassWithPermission.class.getMethod("methodWithoutAnnotation");
        when(methodInvocation.getMethod()).thenReturn(method);
        when(permissionService.hasPermission(authentication, "WRITE_DATA")).thenReturn(true);

        Supplier<Authentication> authSupplier = () -> authentication;

        // When
        AuthorizationResult result = authorizationManager.authorize(authSupplier, methodInvocation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isTrue();
        verify(permissionService).hasPermission(authentication, "WRITE_DATA");
    }

    @Test
    @DisplayName("Should prioritize method-level annotation over class-level annotation")
    void shouldPrioritizeMethodLevelAnnotationOverClassLevelAnnotation() throws NoSuchMethodException {
        // Given
        Method method = ClassWithPermission.class.getMethod("methodWithDifferentPermission");
        when(methodInvocation.getMethod()).thenReturn(method);
        when(permissionService.hasPermission(authentication, "DELETE_DATA")).thenReturn(true);

        Supplier<Authentication> authSupplier = () -> authentication;

        // When
        AuthorizationResult result = authorizationManager.authorize(authSupplier, methodInvocation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isTrue();
        verify(permissionService).hasPermission(authentication, "DELETE_DATA");
        verify(permissionService, never()).hasPermission(authentication, "WRITE_DATA");
    }

    @Test
    @DisplayName("Should deny access when no annotation is found")
    void shouldDenyAccessWhenNoAnnotationIsFound() throws NoSuchMethodException {
        // Given
        Method method = TestClass.class.getMethod("methodWithoutAnnotation");
        when(methodInvocation.getMethod()).thenReturn(method);

        Supplier<Authentication> authSupplier = () -> authentication;

        // When
        AuthorizationResult result = authorizationManager.authorize(authSupplier, methodInvocation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
        verify(permissionService, never()).hasPermission(any(), anyString());
    }

    @Test
    @DisplayName("Should handle null authentication gracefully")
    void shouldHandleNullAuthenticationGracefully() throws NoSuchMethodException {
        // Given
        Method method = TestClass.class.getMethod("methodWithPermission");
        when(methodInvocation.getMethod()).thenReturn(method);
        when(permissionService.hasPermission(null, "READ_DATA")).thenReturn(false);

        Supplier<Authentication> authSupplier = () -> null;

        // When
        AuthorizationResult result = authorizationManager.authorize(authSupplier, methodInvocation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
        verify(permissionService).hasPermission(null, "READ_DATA");
    }

    // Test classes
    public static class TestClass {
        @RequirePermission("READ_DATA")
        public void methodWithPermission() {
            // Empty method used only for testing authorization annotations
        }

        public void methodWithoutAnnotation() {
            // Empty method used only for testing authorization behavior when annotation is absent
        }
    }

    @RequirePermission("WRITE_DATA")
    public static class ClassWithPermission {
        public void methodWithoutAnnotation() {
            // Empty method used only for testing class-level authorization annotations
        }

        @RequirePermission("DELETE_DATA")
        public void methodWithDifferentPermission() {
            // Empty method used only for testing method-level vs class-level annotation priority
        }
    }
}

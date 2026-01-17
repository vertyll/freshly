package com.vertyll.freshly.security.config;

import java.lang.annotation.Annotation;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import lombok.RequiredArgsConstructor;

import com.vertyll.freshly.common.annotation.*;
import com.vertyll.freshly.security.authorization.*;

/** Configuration for method-level security with custom permission and role annotations. */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class MethodSecurityConfig {

    private final PermissionAuthorizationManager permissionAuthorizationManager;
    private final AnyPermissionAuthorizationManager anyPermissionAuthorizationManager;
    private final RoleAuthorizationManager roleAuthorizationManager;
    private final AnyRoleAuthorizationManager anyRoleAuthorizationManager;
    private final AllRolesAuthorizationManager allRolesAuthorizationManager;

    // Permission-based authorization
    @Bean
    public Advisor requirePermissionAuthorizationAdvisor() {
        AspectJExpressionPointcut pointcut = createPointcutForAnnotation(RequirePermission.class);
        AuthorizationManagerBeforeMethodInterceptor interceptor =
                new AuthorizationManagerBeforeMethodInterceptor(
                        pointcut, permissionAuthorizationManager);
        return new DefaultPointcutAdvisor(pointcut, interceptor);
    }

    @Bean
    public Advisor requireAnyPermissionAuthorizationAdvisor() {
        AspectJExpressionPointcut pointcut =
                createPointcutForAnnotation(RequireAnyPermission.class);
        AuthorizationManagerBeforeMethodInterceptor interceptor =
                new AuthorizationManagerBeforeMethodInterceptor(
                        pointcut, anyPermissionAuthorizationManager);
        return new DefaultPointcutAdvisor(pointcut, interceptor);
    }

    // Role-based authorization
    @Bean
    public Advisor requireRoleAuthorizationAdvisor() {
        AspectJExpressionPointcut pointcut = createPointcutForAnnotation(RequireRole.class);
        AuthorizationManagerBeforeMethodInterceptor interceptor =
                new AuthorizationManagerBeforeMethodInterceptor(pointcut, roleAuthorizationManager);
        return new DefaultPointcutAdvisor(pointcut, interceptor);
    }

    @Bean
    public Advisor requireAnyRoleAuthorizationAdvisor() {
        AspectJExpressionPointcut pointcut = createPointcutForAnnotation(RequireAnyRole.class);
        AuthorizationManagerBeforeMethodInterceptor interceptor =
                new AuthorizationManagerBeforeMethodInterceptor(
                        pointcut, anyRoleAuthorizationManager);
        return new DefaultPointcutAdvisor(pointcut, interceptor);
    }

    @Bean
    public Advisor requireAllRolesAuthorizationAdvisor() {
        AspectJExpressionPointcut pointcut = createPointcutForAnnotation(RequireAllRoles.class);
        AuthorizationManagerBeforeMethodInterceptor interceptor =
                new AuthorizationManagerBeforeMethodInterceptor(
                        pointcut, allRolesAuthorizationManager);
        return new DefaultPointcutAdvisor(pointcut, interceptor);
    }

    private AspectJExpressionPointcut createPointcutForAnnotation(
            Class<? extends Annotation> annotationClass) {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        String annotationName = annotationClass.getName();
        pointcut.setExpression(
                "@annotation(" + annotationName + ") || @within(" + annotationName + ")");
        return pointcut;
    }
}

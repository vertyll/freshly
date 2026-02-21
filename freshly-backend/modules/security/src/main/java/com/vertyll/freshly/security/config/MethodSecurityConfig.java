package com.vertyll.freshly.security.config;

import java.lang.annotation.Annotation;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Role;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import com.vertyll.freshly.common.annotation.*;
import com.vertyll.freshly.security.authorization.*;

/** Configuration for method-level security with custom permission and role annotations. */
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
    private final PermissionAuthorizationManager permissionAuthorizationManager;
    private final AnyPermissionAuthorizationManager anyPermissionAuthorizationManager;

    public MethodSecurityConfig(
            @Lazy PermissionAuthorizationManager permissionAuthorizationManager,
            @Lazy AnyPermissionAuthorizationManager anyPermissionAuthorizationManager) {
        this.permissionAuthorizationManager = permissionAuthorizationManager;
        this.anyPermissionAuthorizationManager = anyPermissionAuthorizationManager;
    }

    // Permission-based authorization
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public Advisor requirePermissionAuthorizationAdvisor() {
        AspectJExpressionPointcut pointcut = createPointcutForAnnotation(RequirePermission.class);
        AuthorizationManagerBeforeMethodInterceptor interceptor =
                new AuthorizationManagerBeforeMethodInterceptor(
                        pointcut, permissionAuthorizationManager);
        return new DefaultPointcutAdvisor(pointcut, interceptor);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public Advisor requireAnyPermissionAuthorizationAdvisor() {
        AspectJExpressionPointcut pointcut =
                createPointcutForAnnotation(RequireAnyPermission.class);
        AuthorizationManagerBeforeMethodInterceptor interceptor =
                new AuthorizationManagerBeforeMethodInterceptor(
                        pointcut, anyPermissionAuthorizationManager);
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

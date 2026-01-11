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

import com.vertyll.freshly.security.annotation.RequireAnyPermission;
import com.vertyll.freshly.security.annotation.RequirePermission;
import com.vertyll.freshly.security.authorization.AnyPermissionAuthorizationManager;
import com.vertyll.freshly.security.authorization.PermissionAuthorizationManager;

/**
 * Configuration for method-level security with custom permission annotations. Required for Spring
 * Security 7.0+ which changed how custom annotations work.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class MethodSecurityConfig {

    private final PermissionAuthorizationManager permissionAuthorizationManager;
    private final AnyPermissionAuthorizationManager anyPermissionAuthorizationManager;

    /** Register authorization interceptor for @RequirePermission annotation. */
    @Bean
    public Advisor requirePermissionAuthorizationAdvisor() {
        AspectJExpressionPointcut pointcut = createPointcutForAnnotation(RequirePermission.class);

        AuthorizationManagerBeforeMethodInterceptor interceptor =
                new AuthorizationManagerBeforeMethodInterceptor(
                        pointcut, permissionAuthorizationManager);

        return new DefaultPointcutAdvisor(pointcut, interceptor);
    }

    /** Register authorization interceptor for @RequireAnyPermission annotation. */
    @Bean
    public Advisor requireAnyPermissionAuthorizationAdvisor() {
        AspectJExpressionPointcut pointcut =
                createPointcutForAnnotation(RequireAnyPermission.class);

        AuthorizationManagerBeforeMethodInterceptor interceptor =
                new AuthorizationManagerBeforeMethodInterceptor(
                        pointcut, anyPermissionAuthorizationManager);

        return new DefaultPointcutAdvisor(pointcut, interceptor);
    }

    /**
     * Create a pointcut that matches methods or classes annotated with the given annotation.
     *
     * @param annotationClass the annotation class to match
     * @return configured AspectJ pointcut
     */
    private AspectJExpressionPointcut createPointcutForAnnotation(
            Class<? extends Annotation> annotationClass) {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        String annotationName = annotationClass.getName();
        pointcut.setExpression(
                "@annotation(" + annotationName + ") || @within(" + annotationName + ")");
        return pointcut;
    }
}

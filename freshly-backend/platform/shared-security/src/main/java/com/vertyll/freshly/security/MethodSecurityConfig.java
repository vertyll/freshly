package com.vertyll.freshly.security;

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

import com.vertyll.freshly.authz.PermissionEvaluator;
import com.vertyll.freshly.web.security.RequireAnyPermission;
import com.vertyll.freshly.web.security.RequirePermission;

/**
 * Wires the permission annotations into Spring Security's method interception.
 *
 * <p>
 * One manager for both annotations: {@link PermissionMethodAuthorizationManager}
 * handles {@code @RequirePermission} and {@code @RequireAnyPermission} alike.
 *
 * <p>
 * Note the injected type: {@link PermissionEvaluator}, the platform's own SPI, not the
 * permission context's query port. That is what keeps this file — and the whole platform
 * — compilable without any bounded context on the path. Spring resolves it to whichever
 * bean implements it, which today is `permission`'s `PermissionQueryUseCase`.
 */
@Configuration
@EnableMethodSecurity
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class MethodSecurityConfig {

    @Bean
    PermissionMethodAuthorizationManager permissionMethodAuthorizationManager(@Lazy PermissionEvaluator permissions) {
        return new PermissionMethodAuthorizationManager(permissions);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    Advisor requirePermissionAdvisor(@Lazy PermissionMethodAuthorizationManager manager) {
        return advisorFor(RequirePermission.class, manager);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    Advisor requireAnyPermissionAdvisor(@Lazy PermissionMethodAuthorizationManager manager) {
        return advisorFor(RequireAnyPermission.class, manager);
    }

    private static Advisor advisorFor(
        Class<? extends Annotation> annotation,
        PermissionMethodAuthorizationManager manager
    ) {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        String name = annotation.getName();
        // Both forms, so the annotation works on a class as well as on a method.
        pointcut.setExpression("@annotation(" + name + ") || @within(" + name + ")");

        return new DefaultPointcutAdvisor(pointcut, new AuthorizationManagerBeforeMethodInterceptor(pointcut, manager));
    }
}

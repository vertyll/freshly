package com.vertyll.freshly.security;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.vertyll.freshly.web.security.PublicEndpoint;

import lombok.extern.slf4j.Slf4j;

/**
 * Finds every handler annotated {@code @PublicEndpoint} and turns it into a matcher.
 *
 * <p>
 * Derived rather than declared, so a public path lives in one place — on the handler —
 * instead of in a controller and again in a list here that can drift from it. It also makes
 * {@code @PublicEndpoint} the mechanism rather than documentation, and its mandatory
 * justification string something a reviewer sees at the point of decision.
 */
@Component
@Slf4j
public class PublicEndpointRegistry {
    private final List<RequestMatcher> matchers;

    public PublicEndpointRegistry(
        @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping
    ) {
        this.matchers = discover(handlerMapping);
    }

    public List<RequestMatcher> matchers() {
        return matchers;
    }

    private static List<RequestMatcher> discover(RequestMappingHandlerMapping handlerMapping) {
        List<RequestMatcher> discovered = new ArrayList<>();
        Set<String> described = new LinkedHashSet<>();

        handlerMapping.getHandlerMethods().forEach((info, handler) -> {
            if (!isPublic(handler)) {
                return;
            }
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            patternsOf(info).forEach(pattern -> {
                if (methods.isEmpty()) {
                    discovered.add(PathPatternRequestMatcher.withDefaults().matcher(pattern));
                    described.add("ANY " + pattern);
                    return;
                }
                methods.forEach(method -> {
                    discovered.add(
                        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.valueOf(method.name()), pattern)
                    );
                    described.add(method.name() + " " + pattern);
                });
            });
        });

        log.info("Public endpoints ({}): {}", described.size(), described);

        return List.copyOf(discovered);
    }

    private static boolean isPublic(HandlerMethod handler) {
        return AnnotatedElementUtils.hasAnnotation(handler.getMethod(), PublicEndpoint.class)
                || AnnotatedElementUtils.hasAnnotation(handler.getBeanType(), PublicEndpoint.class);
    }

    private static Set<String> patternsOf(RequestMappingInfo info) {
        PathPatternsRequestCondition condition = info.getPathPatternsCondition();
        if (condition == null) {
            throw new IllegalStateException(
                "Handler " + info + " has no parsed path patterns; @PublicEndpoint needs PathPatternParser"
            );
        }
        return condition.getPatternValues();
    }
}

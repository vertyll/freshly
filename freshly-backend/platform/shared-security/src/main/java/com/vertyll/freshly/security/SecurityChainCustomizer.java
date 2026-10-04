package com.vertyll.freshly.security;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * A module's addition to the one filter chain.
 *
 * <p>
 * The chain belongs to the platform, so a module that needs more than a bearer token — the
 * browser sign-in of {@code auth} — contributes a bean of this type instead of a second chain.
 * Customizers run after the platform's own configuration, so a later setting wins.
 */
@FunctionalInterface
public interface SecurityChainCustomizer extends Customizer<HttpSecurity> {
}

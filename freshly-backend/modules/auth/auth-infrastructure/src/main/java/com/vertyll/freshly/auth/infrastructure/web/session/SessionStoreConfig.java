package com.vertyll.freshly.auth.infrastructure.web.session;

import java.net.URL;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson.SecurityJacksonModules;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * How the attributes of a session are written to Redis: as JSON, never as Java
 * serialization.
 *
 * <p>
 * A session holds the security context and the {@code OAuth2AuthorizedClient} with the
 * user's tokens. Java serialization would tie each stored session to the exact classes of
 * the release that wrote it, and reading it back would instantiate whatever classes its
 * bytes name. Spring Security's Jackson modules map its own types and allow only those —
 * and the collections they hold — as polymorphic types, so a stored value naming any other
 * class is refused.
 *
 * <p>
 * Spring Session picks the serializer up by its bean name.
 */
@Configuration(proxyBeanMethods = false)
public class SessionStoreConfig {
    /**
     * The collections {@code List.of}, {@code Set.of} and {@code Map.of} return, which the
     * claims of a sign-in's ID token can be held in. Spring Security's own list allows only
     * the collections it creates itself.
     */
    private static final String JDK_IMMUTABLE_COLLECTIONS = "java.util.ImmutableCollections$";

    @Bean
    RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return sessionSerializer();
    }

    static RedisSerializer<Object> sessionSerializer() {
        return new GenericJacksonJsonRedisSerializer(
            JsonMapper.builder()
                .addModules(
                    SecurityJacksonModules.getModules(
                        Thread.currentThread().getContextClassLoader(),
                        BasicPolymorphicTypeValidator.builder()
                            .allowIfSubType(JDK_IMMUTABLE_COLLECTIONS)
                            .allowIfSubType(URL.class)
                    )
                )
                .build()
        );
    }
}

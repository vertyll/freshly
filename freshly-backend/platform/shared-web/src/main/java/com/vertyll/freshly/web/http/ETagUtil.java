package com.vertyll.freshly.web.http;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.lang.error.DomainException;

/**
 * Builds and parses the weak ETag that carries an aggregate's version.
 *
 * <p>
 * Only the header format lives here. The version comparison itself is
 * {@code shared-lang}'s {@code VersionGuard}, because the comparison is a rule the
 * domain enforces and the header is a transport detail. Keeping both in one class is
 * how a shared helper ends up throwing Spring's
 * {@code OptimisticLockingFailureException} from inside the hexagon.
 */
public final class ETagUtil {
    private static final String WEAK_PREFIX = "W/\"";
    private static final String QUOTE = "\"";

    private static final String ANY = "*";

    private static final Pattern ENTITY_TAG = Pattern.compile("^(?:W/)?\"(\\d{1,18})\"$");

    private ETagUtil() {
    }

    /**
     * The weak ETag for a stored aggregate's version.
     *
     * <p>
     * Accepts null only to refuse it: an
     * ETag is built for something that has been saved, and a null version means the
     * caller is answering with an aggregate that never was. Inventing {@code W/"0"} for it
     * would hand the client a precondition that matches nothing.
     *
     * @throws IllegalStateException if {@code version} is null
     */
    public static String buildWeakETag(@Nullable Long version) {
        if (version == null) {
            throw new IllegalStateException("An ETag needs a stored version; this aggregate was never saved");
        }
        return WEAK_PREFIX + version + QUOTE;
    }

    /**
     * Reads the version out of an {@code If-Match} header.
     *
     * <p>
     * Null for a missing header, and for {@code *}: both mean the caller asks for no
     * version check, and the use case still refuses a resource that does not exist.
     *
     * <p>
     * Anything else that is not one entity-tag is refused rather than ignored. A client
     * that sent {@code If-Match} asked for a precondition, and treating an unreadable one
     * as absent would let to write through unchecked — the one outcome the header exists
     * to prevent. Matched whole, so {@code "1"junk} is refused rather than read as 1.
     *
     * @throws DomainException with {@link IfMatchError#MALFORMED} for an unreadable value
     */
    @Nullable
    public static Long parseVersion(@Nullable String ifMatchHeader) {
        if (ifMatchHeader == null) {
            return null;
        }
        String value = ifMatchHeader.strip();
        if (ANY.equals(value)) {
            return null;
        }
        Matcher matcher = ENTITY_TAG.matcher(value);
        if (!matcher.matches()) {
            throw new DomainException(IfMatchError.MALFORMED, Map.of("value", value));
        }
        return Long.valueOf(matcher.group(1));
    }
}

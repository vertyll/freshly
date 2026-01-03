package com.vertyll.freshly.common.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Helper class for retrieving internationalized messages.
 * Simplifies access to MessageSource in services and components.
 * Usage in services:
 * <pre>
 * {@code
 * @Service
 * public class SomeService {
 *     private final MessageHelper messageHelper;
 *     
 *     public SomeService(MessageHelper messageHelper) {
 *         this.messageHelper = messageHelper;
 *     }
 *     
 *     public void someMethod() {
 *         String message = messageHelper.getMessage("error.user.notFound");
 *         // Use message...
 *     }
 * }
 * }
 * </pre>
 */
@Component
public class MessageHelper {

    private final MessageSource messageSource;

    public MessageHelper(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Gets localized message for current request locale.
     * 
     * @param key Message key from messages_*.properties
     * @return Localized message
     */
    public String getMessage(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }

    /**
     * Gets localized message with parameters for current request locale.
     * 
     * @param key Message key from messages_*.properties
     * @param args Arguments to replace placeholders in message
     * @return Localized message with substituted parameters
     */
    public String getMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    /**
     * Gets localized message for specific locale.
     * 
     * @param key Message key from messages_*.properties
     * @param locale Specific locale to use
     * @return Localized message for given locale
     */
    public String getMessage(String key, Locale locale) {
        return messageSource.getMessage(key, null, locale);
    }

    /**
     * Gets localized message with parameters for specific locale.
     * 
     * @param key Message key from messages_*.properties
     * @param args Arguments to replace placeholders in message
     * @param locale Specific locale to use
     * @return Localized message with substituted parameters for given locale
     */
    public String getMessage(String key, Object[] args, Locale locale) {
        return messageSource.getMessage(key, args, locale);
    }
}

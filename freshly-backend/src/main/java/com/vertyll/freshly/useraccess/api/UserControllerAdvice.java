package com.vertyll.freshly.useraccess.api;

import com.vertyll.freshly.useraccess.domain.exception.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = UserController.class)
public class UserControllerAdvice {

    private final MessageSource messageSource;

    private static final Logger logger = LogManager.getLogger(UserControllerAdvice.class);

    public UserControllerAdvice(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        logger.warn("User not found: {}", ex.getMessage());
        String message = messageSource.getMessage("error.user.notFound", null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, message);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex) {
        logger.warn("User already exists: {}", ex.getMessage());
        String message = messageSource.getMessage("error.user.alreadyExists", null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler(UserAlreadyActiveException.class)
    public ProblemDetail handleUserAlreadyActive(UserAlreadyActiveException ex) {
        logger.warn("User already active: {}", ex.getMessage());
        String message = messageSource.getMessage("error.user.alreadyActive", null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(UserAlreadyInactiveException.class)
    public ProblemDetail handleUserAlreadyInactive(UserAlreadyInactiveException ex) {
        logger.warn("User already inactive: {}", ex.getMessage());
        String message = messageSource.getMessage("error.user.alreadyInactive", null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(SelfDeactivationException.class)
    public ProblemDetail handleSelfDeactivation(SelfDeactivationException ex) {
        logger.warn("Self deactivation attempt: {}", ex.getMessage());
        String message = messageSource.getMessage("error.user.selfDeactivation", null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, message);
    }

    @ExceptionHandler(UserRolesEmptyException.class)
    public ProblemDetail handleUserRolesEmpty(UserRolesEmptyException ex) {
        logger.warn("User roles empty: {}", ex.getMessage());
        String message = messageSource.getMessage("error.user.rolesEmpty", null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
    }
}

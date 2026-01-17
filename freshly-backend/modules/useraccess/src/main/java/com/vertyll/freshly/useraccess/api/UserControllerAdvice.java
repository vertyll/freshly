package com.vertyll.freshly.useraccess.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.vertyll.freshly.useraccess.domain.exception.*;

@RestControllerAdvice(assignableTypes = UserController.class)
class UserControllerAdvice {
    private static final String USER_NOT_FOUND_MSG_KEY = "error.user.notFound";
    private static final String USER_ALREADY_EXISTS_MSG_KEY = "error.user.alreadyExists";
    private static final String USER_ALREADY_ACTIVE_MSG_KEY = "error.user.alreadyActive";
    private static final String USER_ALREADY_INACTIVE_MSG_KEY = "error.user.alreadyInactive";
    private static final String SELF_DEACTIVATION_MSG_KEY = "error.user.selfDeactivation";
    private static final String USER_ROLES_EMPTY_MSG_KEY = "error.user.rolesEmpty";
    private static final String OPTIMISTIC_LOCKING_FAILURE_MSG_KEY =
            "error.common.optimisticLockingFailure";

    private final MessageSource messageSource;

    private static final Logger LOGGER = LogManager.getLogger(UserControllerAdvice.class);

    public UserControllerAdvice(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        LOGGER.warn("User not found: {}", ex.getMessage());
        String message =
                messageSource.getMessage(
                        USER_NOT_FOUND_MSG_KEY, null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, message);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex) {
        LOGGER.warn("User already exists: {}", ex.getMessage());
        String message =
                messageSource.getMessage(
                        USER_ALREADY_EXISTS_MSG_KEY, null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler(UserAlreadyActiveException.class)
    public ProblemDetail handleUserAlreadyActive(UserAlreadyActiveException ex) {
        LOGGER.warn("User already active: {}", ex.getMessage());
        String message =
                messageSource.getMessage(
                        USER_ALREADY_ACTIVE_MSG_KEY, null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(UserAlreadyInactiveException.class)
    public ProblemDetail handleUserAlreadyInactive(UserAlreadyInactiveException ex) {
        LOGGER.warn("User already inactive: {}", ex.getMessage());
        String message =
                messageSource.getMessage(
                        USER_ALREADY_INACTIVE_MSG_KEY, null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(SelfDeactivationException.class)
    public ProblemDetail handleSelfDeactivation(SelfDeactivationException ex) {
        LOGGER.warn("Self deactivation attempt: {}", ex.getMessage());
        String message =
                messageSource.getMessage(
                        SELF_DEACTIVATION_MSG_KEY, null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, message);
    }

    @ExceptionHandler(UserRolesEmptyException.class)
    public ProblemDetail handleUserRolesEmpty(UserRolesEmptyException ex) {
        LOGGER.warn("User roles empty: {}", ex.getMessage());
        String message =
                messageSource.getMessage(
                        USER_ROLES_EMPTY_MSG_KEY, null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockingFailure(OptimisticLockingFailureException ex) {
        LOGGER.warn("Optimistic locking failure: {}", ex.getMessage());
        String message =
                messageSource.getMessage(
                        OPTIMISTIC_LOCKING_FAILURE_MSG_KEY, null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, message);
    }
}

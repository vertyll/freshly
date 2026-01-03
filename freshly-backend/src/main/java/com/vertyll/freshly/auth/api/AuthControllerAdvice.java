package com.vertyll.freshly.auth.api;

import com.vertyll.freshly.auth.domain.exception.EmailAlreadyExistsException;
import com.vertyll.freshly.auth.domain.exception.InvalidPasswordException;
import com.vertyll.freshly.auth.domain.exception.InvalidVerificationTokenException;
import com.vertyll.freshly.auth.domain.exception.UsernameAlreadyExistsException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthControllerAdvice {

    private final MessageSource messageSource;
    private static final Logger logger = LogManager.getLogger(AuthControllerAdvice.class);

    public AuthControllerAdvice(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ProblemDetail handleUsernameAlreadyExists(UsernameAlreadyExistsException ex) {
        logger.warn("Username conflict: {}", ex.getMessage());
        String message = messageSource.getMessage("error.auth.usernameExists", null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        logger.warn("Email conflict: {}", ex.getMessage());
        String message = messageSource.getMessage("error.auth.emailExists", null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler(InvalidVerificationTokenException.class)
    public ProblemDetail handleInvalidToken(InvalidVerificationTokenException ex) {
        logger.warn("Invalid verification token: {}", ex.getMessage());
        String message = messageSource.getMessage("error.auth.invalidToken", null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ProblemDetail handleInvalidPassword(InvalidPasswordException ex) {
        logger.warn("Invalid password: {}", ex.getMessage());
        String message = messageSource.getMessage("error.auth.invalidPassword", null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, message);
    }
}

package com.vertyll.freshly.auth.application.port.inbound.command;

import com.vertyll.freshly.auth.application.command.ChangeEmailCommand;
import com.vertyll.freshly.auth.application.command.ChangePasswordCommand;
import com.vertyll.freshly.auth.application.command.ResetPasswordCommand;

public interface CredentialsUseCase {
    void changePassword(ChangePasswordCommand command);

    void initiatePasswordReset(String email, String languageTag);

    void resetPassword(ResetPasswordCommand command);

    void changeEmail(ChangeEmailCommand command);
}

package com.vertyll.freshly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.vertyll.freshly.airquality.infrastructure.config.AirQualityModuleConfig;
import com.vertyll.freshly.auth.infrastructure.config.AuthModuleConfig;
import com.vertyll.freshly.infra.SharedInfraConfig;
import com.vertyll.freshly.notification.infrastructure.config.NotificationModuleConfig;
import com.vertyll.freshly.permission.infrastructure.config.PermissionModuleConfig;
import com.vertyll.freshly.security.SecurityPlatformConfig;
import com.vertyll.freshly.translation.infrastructure.config.TranslationModuleConfig;
import com.vertyll.freshly.useraccess.infrastructure.config.UserAccessModuleConfig;
import com.vertyll.freshly.web.WebPlatformConfig;

/**
 * The one process.
 *
 * <p>
 * Here each module states its own contribution in a {@code *ModuleConfig}, and
 * this class names the modules it assembles. Adding a bounded context is one line
 * here, which is also the moment somebody notices it is being added.
 */
@SpringBootApplication
@EnableScheduling
@Import(
    {
        SharedInfraConfig.class,
        WebPlatformConfig.class,
        SecurityPlatformConfig.class,
        UserAccessModuleConfig.class,
        NotificationModuleConfig.class,
        PermissionModuleConfig.class,
        AirQualityModuleConfig.class,
        TranslationModuleConfig.class,
        AuthModuleConfig.class
    }
)
public class FreshlyApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreshlyApplication.class, args);
    }
}

package com.trainticket.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean public OpenAPI trackEaseApi() {
        return new OpenAPI().info(new Info().title("TrackEase API").version("1.0")
                .description("Railway reservations with automatic seats, cancellation and FIFO waiting lists. "
                    + "To try requests: execute GET /api/auth/csrf, copy its token into Authorize > csrfToken, "
                    + "then execute POST /api/auth/login. Login returns a fresh csrfToken: update Authorize before further writes. "
                    + "The browser manages the session cookie automatically. Use the same hostname for the app and these docs. "
                    + "Admin routes require an administrator account. Logout invalidates both the session and its token."))
                .components(new Components()
                    .addSecuritySchemes("sessionCookie",new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE).name("JSESSIONID")
                        .description("Managed automatically by the browser after login. No manual cookie entry is needed."))
                    .addSecuritySchemes("csrfToken",new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER).name("X-CSRF-Token")
                        .description("Paste the token from GET /api/auth/csrf. Replace it with login's csrfToken after signing in.")));
    }

    @Bean public OpenApiCustomizer sessionAndCsrfRequirements() {
        return api -> api.getPaths().forEach((path,item) -> item.readOperationsMap().forEach((method,operation) -> {
            boolean authenticated=path.startsWith("/api/admin/") || path.startsWith("/api/bookings")
                || path.startsWith("/api/journeys/") || path.startsWith("/api/account/") || path.equals("/api/auth/me");
            boolean unsafe=switch(method) { case POST,PUT,PATCH,DELETE -> true; default -> false; };
            SecurityRequirement requirement=new SecurityRequirement();
            if(authenticated) { requirement.addList("sessionCookie"); }
            if(unsafe) { requirement.addList("csrfToken"); }
            operation.setSecurity(requirement.isEmpty()?java.util.List.of():java.util.List.of(requirement));
        }));
    }
}

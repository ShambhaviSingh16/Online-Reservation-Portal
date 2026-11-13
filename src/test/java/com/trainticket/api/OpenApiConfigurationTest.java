package com.trainticket.api;

import com.trainticket.config.OpenApiConfiguration;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigurationTest {
    @Test void documentationMatchesSessionAndCsrfRules() {
        var config=new OpenApiConfiguration();
        var api=config.trackEaseApi().paths(new Paths()
            .addPathItem("/api/auth/csrf",new PathItem().get(new Operation()))
            .addPathItem("/api/auth/login",new PathItem().post(new Operation()))
            .addPathItem("/api/auth/me",new PathItem().get(new Operation()))
            .addPathItem("/api/bookings",new PathItem().post(new Operation()))
            .addPathItem("/api/bookings/{id}",new PathItem().get(new Operation()).delete(new Operation()))
            .addPathItem("/api/admin/analytics",new PathItem().get(new Operation())));
        config.sessionAndCsrfRequirements().customise(api);
        assertEquals("TrackEase API",api.getInfo().getTitle());
        assertEquals("X-CSRF-Token",api.getComponents().getSecuritySchemes().get("csrfToken").getName());
        assertEquals(SecurityScheme.In.COOKIE,api.getComponents().getSecuritySchemes().get("sessionCookie").getIn());
        assertTrue(api.getPaths().get("/api/auth/csrf").getGet().getSecurity().isEmpty());
        assertEquals(java.util.Set.of("csrfToken"),api.getPaths().get("/api/auth/login").getPost().getSecurity().get(0).keySet());
        assertEquals(java.util.Set.of("sessionCookie"),api.getPaths().get("/api/auth/me").getGet().getSecurity().get(0).keySet());
        assertEquals(java.util.Set.of("sessionCookie","csrfToken"),api.getPaths().get("/api/bookings").getPost().getSecurity().get(0).keySet());
        assertEquals(java.util.Set.of("sessionCookie","csrfToken"),api.getPaths().get("/api/bookings/{id}").getDelete().getSecurity().get(0).keySet());
        assertEquals(java.util.Set.of("sessionCookie"),api.getPaths().get("/api/admin/analytics").getGet().getSecurity().get(0).keySet());
    }
}

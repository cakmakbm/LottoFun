package com.bmc.lottofun.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kubernetes calls the liveness and readiness probes without a JWT, so they must be public.
 * Nothing else under /actuator may be.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthEndpointsIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("Liveness and readiness probes respond UP without authentication")
    void probes_arePublicAndUp() throws Exception {
        for (String path : new String[]{"/actuator/health/liveness", "/actuator/health/readiness"}) {
            HttpResponse<String> response = get(path);
            assertThat(response.statusCode()).as(path).isEqualTo(200);
            assertThat(response.body()).as(path).contains("\"status\":\"UP\"");
        }
    }

    @Test
    @DisplayName("Other actuator endpoints are not publicly reachable")
    void otherActuatorEndpoints_areNotPublic() throws Exception {
        assertThat(get("/actuator/env").statusCode()).isNotEqualTo(200);
        assertThat(get("/actuator/beans").statusCode()).isNotEqualTo(200);
    }
}

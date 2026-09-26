package com.vertyll.freshly;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MongoTestContainer.class)
class FreshlyApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void startsAndReportsHealthy() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void servesPublicEndpointsWithoutToken() throws Exception {
        mockMvc.perform(get("/translations/languages")).andExpect(status().isOk());
    }

    @Test
    void rejectsProtectedEndpointsWithoutToken() throws Exception {
        mockMvc.perform(get("/users")).andExpect(status().isUnauthorized());
    }
}

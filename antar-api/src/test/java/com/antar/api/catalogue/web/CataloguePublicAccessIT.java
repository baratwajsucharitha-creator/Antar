package com.antar.api.catalogue.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * With antar.auth.require-platform-principal=true (the azure profile's real setting), a
 * request with no X-MS-CLIENT-PRINCIPAL-ID header is rejected for owner-scoped endpoints
 * like /api/v1/policies (401 - see CurrentUserArgumentResolver). The catalogue endpoints
 * take no CurrentUser parameter at all, so that resolver never runs for them: they must
 * still answer 200 in this same strict mode. This is the test that actually distinguishes
 * "public reference data" from "happens to work locally because auth is relaxed".
 */
@SpringBootTest(properties = "antar.auth.require-platform-principal=true")
@AutoConfigureMockMvc
class CataloguePublicAccessIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void policiesEndpointRejectsARequestWithNoPrincipalHeaderInStrictMode() throws Exception {
        mockMvc.perform(get("/api/v1/policies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void insurersEndpointStillAnswers200WithNoPrincipalHeaderInStrictMode() throws Exception {
        mockMvc.perform(get("/api/v1/insurers"))
                .andExpect(status().isOk());
    }

    @Test
    void insurerProductsEndpointStillAnswers200WithNoPrincipalHeaderInStrictMode() throws Exception {
        mockMvc.perform(get("/api/v1/insurers/{id}/products", 1L))
                .andExpect(status().isNotFound()); // no such insurer, but NOT 401 - it got past auth
    }

    @Test
    void productVersionsEndpointStillAnswers200OrNotFoundWithNoPrincipalHeaderInStrictMode() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}/versions", 1L))
                .andExpect(status().isNotFound()); // no such product, but NOT 401 - it got past auth
    }
}

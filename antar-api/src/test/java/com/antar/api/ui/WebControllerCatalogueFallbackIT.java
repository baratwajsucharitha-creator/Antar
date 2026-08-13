package com.antar.api.ui;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The catalogue insurer/product dropdowns (see policy.html) are server-rendered
 * from the catalogue tables - they must never be able to block the form. This
 * test covers that guarantee with the catalogue tables empty (no CSV import has
 * run, "test" profile keeps import-on-startup off): the page renders no
 * <select id="insurerSelect">/<select id="productSelect"> at all, only the plain
 * free-text inputs, and the form still submits successfully via free text alone.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WebControllerCatalogueFallbackIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void policyFormRendersPlainTextInputsWithNoCatalogueSelectsWhenCatalogueIsEmpty() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"insurerName\"")))
                .andExpect(content().string(containsString("id=\"productName\"")))
                .andExpect(content().string(not(containsString("id=\"insurerSelect\""))))
                .andExpect(content().string(not(containsString("id=\"productSelect\""))));
    }

    @Test
    void policyFormSubmitsSuccessfullyViaFreeTextWhenCatalogueTablesAreEmpty() throws Exception {
        mockMvc.perform(post("/policy")
                        .param("insurerName", "Free Text Insurer")
                        .param("productName", "Free Text Product")
                        .param("sumInsured", "500000")
                        .param("roomRentPercent", "1.00")
                        .param("proportionPharmacy", "false")
                        .param("coPayPercent", "0")
                        .param("coPayOrder", "AFTER_PROPORTION"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/bill/*"));
    }
}

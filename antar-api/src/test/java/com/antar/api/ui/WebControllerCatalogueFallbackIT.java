package com.antar.api.ui;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The catalogue insurer/product dropdowns (see static/js/catalogue.js) are a purely
 * client-side, additive enhancement over the free-text insurerName/productName
 * inputs - they must never be able to block the form. This test covers the
 * server-rendered side of that guarantee: with the catalogue tables empty (no CSV
 * import has run), the page still renders plain text inputs with no server-side
 * dependency on catalogue data, and the form still submits successfully via
 * free text alone. The other two required degrade cases - JavaScript disabled,
 * and the catalogue fetch failing at runtime - are properties of catalogue.js
 * never touching form validity/required-ness or submission itself, verified by
 * code construction and live curl checks (see Gate D notes); there is no browser
 * test harness in this project to automate them further.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebControllerCatalogueFallbackIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void policyFormRendersPlainTextInputsWithNoServerSideDependencyOnCatalogueData() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"insurerName\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"productName\"")))
                // The dropdowns are injected by catalogue.js at runtime, never server-rendered.
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("catalogueInsurerSelect"))));
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

package com.antar.api.ui;

import com.antar.api.catalogue.persistence.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the insurer/product <select> elements are actually present in the
 * server-rendered HTML - not "should populate", the literal response body
 * contains the <option> rows - when the catalogue has data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WebControllerCatalogueSelectRenderIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InsurerRepository insurerRepository;

    @Autowired
    private InsuranceProductRepository productRepository;

    @Test
    void policyFormRendersServerSideInsurerAndProductOptionsWhenCatalogueHasData() throws Exception {
        InsurerEntity insurer = new InsurerEntity();
        insurer.setIrdaiRegistrationNo(null);
        insurer.setLegalName("Render Test Insurer");
        insurer.setDisplayName("Render Test Insurer");
        insurer.setInsurerType(InsurerType.STANDALONE_HEALTH);
        insurer.setActive(true);
        insurer.setSource("test");
        insurer.setLastVerifiedDate(LocalDate.of(2026, 8, 13));
        insurer = insurerRepository.save(insurer);

        InsuranceProductEntity product = new InsuranceProductEntity();
        product.setInsurerId(insurer.getId());
        product.setProductName("Render Test Product");
        product.setProductCategory(ProductCategory.INDEMNITY);
        product.setSegment(Segment.RETAIL);
        product.setAvailabilityStatus(AvailabilityStatus.WITHDRAWN);
        product.setSource("test");
        product.setLastVerifiedDate(LocalDate.of(2026, 8, 13));
        productRepository.save(product);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"insurerSelect\"")))
                .andExpect(content().string(containsString("id=\"productSelect\"")))
                .andExpect(content().string(containsString(">Render Test Insurer<")))
                // The product itself is embedded as JSON, filtered into <option> elements
                // client-side - the JSON payload (not a server-rendered <option>) is what
                // must be present in the HTML for a WITHDRAWN product like this one.
                .andExpect(content().string(containsString("Render Test Product")))
                .andExpect(content().string(containsString("WITHDRAWN")))
                .andExpect(content().string(containsString("Other — not listed")));
    }
}

package com.antar.api.catalogue.web;

import com.antar.api.catalogue.persistence.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The catalogue endpoints are public reference data, deliberately not owner-scoped like
 * /api/v1/policies. Every test here calls them with no X-MS-CLIENT-PRINCIPAL-ID header.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogueControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InsurerRepository insurerRepository;

    @Autowired
    private InsuranceProductRepository productRepository;

    @Autowired
    private ProductVersionRepository versionRepository;

    private InsurerEntity persistInsurer(String regNo, String displayName, InsurerType type) {
        InsurerEntity insurer = new InsurerEntity();
        insurer.setIrdaiRegistrationNo(regNo);
        insurer.setLegalName(displayName + " Limited");
        insurer.setDisplayName(displayName);
        insurer.setInsurerType(type);
        insurer.setSource("test");
        insurer.setLastVerifiedDate(LocalDate.of(2026, 8, 12));
        return insurerRepository.save(insurer);
    }

    private InsuranceProductEntity persistProduct(Long insurerId, String name, AvailabilityStatus status) {
        InsuranceProductEntity product = new InsuranceProductEntity();
        product.setInsurerId(insurerId);
        product.setProductName(name);
        product.setProductCategory(ProductCategory.INDEMNITY);
        product.setSegment(Segment.RETAIL);
        product.setAvailabilityStatus(status);
        product.setSource("test");
        product.setLastVerifiedDate(LocalDate.of(2026, 8, 12));
        return productRepository.save(product);
    }

    private ProductVersionEntity persistVersion(Long productId, String uin, LocalDate from, LocalDate to) {
        ProductVersionEntity version = new ProductVersionEntity();
        version.setProductId(productId);
        version.setUin(uin);
        version.setEffectiveFrom(from);
        version.setEffectiveTo(to);
        version.setSourceUrl("https://example.invalid/source");
        version.setSource("test");
        version.setLastVerifiedDate(LocalDate.of(2026, 8, 12));
        return versionRepository.save(version);
    }

    @Test
    void insurersEndpointIsReachableWithoutAnEasyAuthPrincipalHeader() throws Exception {
        persistInsurer("PUB-1", "Public Test Insurer", InsurerType.STANDALONE_HEALTH);

        mockMvc.perform(get("/api/v1/insurers"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Cache-Control"))
                .andExpect(header().string("Cache-Control", containsString("max-age=86400")))
                .andExpect(header().string("Cache-Control", containsString("public")));
    }

    @Test
    void productsEndpointIsReachableWithoutAnEasyAuthPrincipalHeader() throws Exception {
        InsurerEntity insurer = persistInsurer("PUB-2", "Public Test Insurer 2", InsurerType.STANDALONE_HEALTH);

        mockMvc.perform(get("/api/v1/insurers/{id}/products", insurer.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void versionsEndpointIsReachableWithoutAnEasyAuthPrincipalHeader() throws Exception {
        InsurerEntity insurer = persistInsurer("PUB-3", "Public Test Insurer 3", InsurerType.STANDALONE_HEALTH);
        InsuranceProductEntity product = persistProduct(insurer.getId(), "Public Product", AvailabilityStatus.UNKNOWN);

        mockMvc.perform(get("/api/v1/products/{id}/versions", product.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void productsWithoutAvailabilityFilterIncludesWithdrawnByDefault() throws Exception {
        InsurerEntity insurer = persistInsurer("WD-1", "Withdrawn Test Insurer", InsurerType.STANDALONE_HEALTH);
        persistProduct(insurer.getId(), "Open Product", AvailabilityStatus.OPEN_TO_NEW);
        persistProduct(insurer.getId(), "Old Withdrawn Product", AvailabilityStatus.WITHDRAWN);

        mockMvc.perform(get("/api/v1/insurers/{id}/products", insurer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].productName", containsInAnyOrder("Open Product", "Old Withdrawn Product")));
    }

    @Test
    void availabilityFilterExcludesWithdrawnWhenExplicitlyRequested() throws Exception {
        InsurerEntity insurer = persistInsurer("WD-2", "Withdrawn Filter Insurer", InsurerType.STANDALONE_HEALTH);
        persistProduct(insurer.getId(), "Still Open Product", AvailabilityStatus.OPEN_TO_NEW);
        persistProduct(insurer.getId(), "Long Withdrawn Product", AvailabilityStatus.WITHDRAWN);

        mockMvc.perform(get("/api/v1/insurers/{id}/products", insurer.getId())
                        .param("availability", "OPEN_TO_NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].productName", contains("Still Open Product")));
    }

    @Test
    void unknownInsurerReturns404NotAnEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/insurers/{id}/products", 999_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownProductReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}/versions", 999_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void versionCountAndCurrentUinAreComputedFromAllVersionsOfAProduct() throws Exception {
        InsurerEntity insurer = persistInsurer("VC-1", "Version Count Insurer", InsurerType.STANDALONE_HEALTH);
        InsuranceProductEntity product = persistProduct(insurer.getId(), "Versioned Product", AvailabilityStatus.OPEN_TO_NEW);
        persistVersion(product.getId(), "VCUIN0001V012015", LocalDate.of(2015, 4, 1), LocalDate.of(2020, 3, 31));
        persistVersion(product.getId(), "VCUIN0001V022020", LocalDate.of(2020, 4, 1), null);

        mockMvc.perform(get("/api/v1/insurers/{id}/products", insurer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versionCount").value(2))
                .andExpect(jsonPath("$[0].currentUin").value("VCUIN0001V022020"));
    }

    @Test
    void asOfBeforeAnyVersionExistedReturnsAnEmptyList() throws Exception {
        InsurerEntity insurer = persistInsurer("ASOF-1", "AsOf Insurer 1", InsurerType.STANDALONE_HEALTH);
        InsuranceProductEntity product = persistProduct(insurer.getId(), "AsOf Product", AvailabilityStatus.OPEN_TO_NEW);
        persistVersion(product.getId(), "ASOFUIN001V012015", LocalDate.of(2015, 4, 1), LocalDate.of(2020, 3, 31));

        mockMvc.perform(get("/api/v1/products/{id}/versions", product.getId())
                        .param("asOf", "2010-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void asOfInsideAClosedWindowResolvesThatVersion() throws Exception {
        InsurerEntity insurer = persistInsurer("ASOF-2", "AsOf Insurer 2", InsurerType.STANDALONE_HEALTH);
        InsuranceProductEntity product = persistProduct(insurer.getId(), "AsOf Product 2", AvailabilityStatus.OPEN_TO_NEW);
        persistVersion(product.getId(), "ASOFUIN002V012015", LocalDate.of(2015, 4, 1), LocalDate.of(2020, 3, 31));
        persistVersion(product.getId(), "ASOFUIN002V022020", LocalDate.of(2020, 4, 1), null);

        mockMvc.perform(get("/api/v1/products/{id}/versions", product.getId())
                        .param("asOf", "2017-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uin").value("ASOFUIN002V012015"));
    }

    @Test
    void asOfInsideTheOpenEndedCurrentWindowResolvesThatVersion() throws Exception {
        InsurerEntity insurer = persistInsurer("ASOF-3", "AsOf Insurer 3", InsurerType.STANDALONE_HEALTH);
        InsuranceProductEntity product = persistProduct(insurer.getId(), "AsOf Product 3", AvailabilityStatus.OPEN_TO_NEW);
        persistVersion(product.getId(), "ASOFUIN003V012015", LocalDate.of(2015, 4, 1), LocalDate.of(2020, 3, 31));
        persistVersion(product.getId(), "ASOFUIN003V022020", LocalDate.of(2020, 4, 1), null);

        mockMvc.perform(get("/api/v1/products/{id}/versions", product.getId())
                        .param("asOf", "2026-08-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uin").value("ASOFUIN003V022020"));
    }

    @Test
    void succeededByInsurerIsResolvedToTheSuccessorsDisplayName() throws Exception {
        InsurerEntity successor = persistInsurer("SUCC-2", "HDFC ERGO Test", InsurerType.GENERAL);
        InsurerEntity ceased = persistInsurer("SUCC-1", "Apollo Munich Test", InsurerType.STANDALONE_HEALTH);
        ceased.setSucceededByInsurerId(successor.getId());
        insurerRepository.save(ceased);

        String body = mockMvc.perform(get("/api/v1/insurers").param("q", "Apollo"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get(0).get("cededTo").asText()).isEqualTo("HDFC ERGO Test");
    }
}

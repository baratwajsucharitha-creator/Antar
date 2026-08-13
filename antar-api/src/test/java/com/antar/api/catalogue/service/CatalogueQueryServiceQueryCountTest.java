package com.antar.api.catalogue.service;

import com.antar.api.catalogue.persistence.*;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The product-list response derives versionCount/currentUin from product_version for
 * every matched product. A naive per-product query would make listProducts's query
 * count scale with the number of products - exactly the N+1 an insurer with a decade
 * of clearance history would hit. CatalogueQueryService.listProducts instead does one
 * bulk fetch of every matched product's versions (see
 * ProductVersionRepository.findByProductIdInOrderByProductIdAscEffectiveFromDesc), so
 * the query count here must be identical whether there are 3 products or 12.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Transactional
class CatalogueQueryServiceQueryCountTest {

    @Autowired
    private CatalogueQueryService queryService;

    @Autowired
    private InsurerRepository insurerRepository;

    @Autowired
    private InsuranceProductRepository productRepository;

    @Autowired
    private ProductVersionRepository versionRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private InsurerEntity persistInsurer(String regNo) {
        InsurerEntity insurer = new InsurerEntity();
        insurer.setIrdaiRegistrationNo(regNo);
        insurer.setLegalName(regNo + " Ltd");
        insurer.setDisplayName(regNo);
        insurer.setInsurerType(InsurerType.STANDALONE_HEALTH);
        insurer.setSource("test");
        insurer.setLastVerifiedDate(LocalDate.of(2026, 8, 12));
        return insurerRepository.save(insurer);
    }

    private void persistProductWithVersions(Long insurerId, String name, int versionCount) {
        InsuranceProductEntity product = new InsuranceProductEntity();
        product.setInsurerId(insurerId);
        product.setProductName(name);
        product.setProductCategory(ProductCategory.INDEMNITY);
        product.setSegment(Segment.RETAIL);
        product.setSource("test");
        product.setLastVerifiedDate(LocalDate.of(2026, 8, 12));
        InsuranceProductEntity saved = productRepository.save(product);

        for (int i = 0; i < versionCount; i++) {
            ProductVersionEntity version = new ProductVersionEntity();
            version.setProductId(saved.getId());
            version.setUin(name.replace(" ", "") + "UIN" + i);
            version.setEffectiveFrom(LocalDate.of(2015 + i, 4, 1));
            version.setSourceUrl("https://example.invalid/source");
            version.setSource("test");
            version.setLastVerifiedDate(LocalDate.of(2026, 8, 12));
            versionRepository.save(version);
        }
    }

    private long queryCountFor(Runnable action) {
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        action.run();
        return stats.getQueryExecutionCount();
    }

    @Test
    void queryCountDoesNotScaleWithTheNumberOfProducts() {
        InsurerEntity small = persistInsurer("QC-SMALL");
        for (int i = 0; i < 3; i++) {
            persistProductWithVersions(small.getId(), "Small Product " + i, 2);
        }

        InsurerEntity large = persistInsurer("QC-LARGE");
        for (int i = 0; i < 12; i++) {
            persistProductWithVersions(large.getId(), "Large Product " + i, 2);
        }

        long smallCount = queryCountFor(() -> queryService.listProducts(small.getId(), null, null));
        long largeCount = queryCountFor(() -> queryService.listProducts(large.getId(), null, null));

        assertThat(largeCount)
                .as("query count for 12 products should equal the count for 3 - both must be O(1), not O(n)")
                .isEqualTo(smallCount);
        // The bulk implementation is: exists check, product list, version batch fetch = 3.
        assertThat(smallCount).isLessThanOrEqualTo(3);
    }
}

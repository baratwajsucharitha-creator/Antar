package com.antar.api.catalogue.web.dto;

/**
 * One row of the full product list embedded in the policy form's HTML as JSON
 * (see policy.html) so the product &lt;select&gt; can be filtered by insurer
 * client-side with no network round trip. availabilityStatus is the raw enum
 * name; the template maps it to a plain-language suffix.
 */
public record ProductOption(Long insurerId, String productName, String availabilityStatus) {
}

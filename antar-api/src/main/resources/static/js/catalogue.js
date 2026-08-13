/*
 * Optional catalogue dropdowns for the policy form. Purely additive: this script
 * only ever inserts extra <select> elements next to the existing free-text
 * insurerName/productName inputs and, when a user picks an option, writes into
 * those same inputs - it never adds a "required" attribute, never disables the
 * text inputs, and never touches form submission itself.
 *
 * That means every failure mode degrades to "the plain text form works exactly
 * as it always has":
 *   - JavaScript disabled entirely: this file never runs. The page the server
 *     rendered has no dropdown in it at all - there is nothing to degrade.
 *   - Catalogue tables empty: /api/v1/insurers returns an empty array. The
 *     early return below fires and nothing is inserted.
 *   - Fetch fails (network error, 5xx, unexpected shape): caught below and
 *     treated the same as "no data" - nothing is inserted, nothing is thrown.
 */
(function () {
    "use strict";

    var STATUS_LABELS = {
        CLOSED_TO_NEW: "Closed to new customers",
        WITHDRAWN: "Withdrawn",
        UNKNOWN: "Availability not confirmed"
        // OPEN_TO_NEW intentionally has no label - that is the unremarkable case.
    };

    function fetchJson(url) {
        return fetch(url, {headers: {Accept: "application/json"}})
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("catalogue request failed: " + response.status);
                }
                return response.json();
            });
    }

    function buildField(labelText, selectId) {
        var field = document.createElement("div");
        field.className = "field catalogue-field";

        var label = document.createElement("label");
        label.setAttribute("for", selectId);
        label.textContent = labelText;

        var select = document.createElement("select");
        select.id = selectId;

        var blank = document.createElement("option");
        blank.value = "";
        blank.textContent = "— choose, or type above —";
        select.appendChild(blank);

        field.appendChild(label);
        field.appendChild(select);
        return {field: field, select: select};
    }

    function removeExistingProductField() {
        var existing = document.getElementById("catalogueProductField");
        if (existing) {
            existing.remove();
        }
    }

    function setupProductDropdown(insurerId, insurerFieldEl, productNameInput) {
        removeExistingProductField();

        fetchJson("/api/v1/insurers/" + encodeURIComponent(insurerId) + "/products")
            .then(function (products) {
                if (!Array.isArray(products) || products.length === 0) {
                    return;
                }

                var built = buildField("Or choose a product from the list", "catalogueProductSelect");
                built.field.id = "catalogueProductField";

                products.forEach(function (product) {
                    var option = document.createElement("option");
                    option.value = product.productName;
                    var label = product.productName;
                    var statusLabel = STATUS_LABELS[product.availabilityStatus];
                    if (statusLabel) {
                        label += " — " + statusLabel;
                    }
                    option.textContent = label;
                    built.select.appendChild(option);
                });

                built.select.addEventListener("change", function () {
                    if (built.select.value !== "") {
                        productNameInput.value = built.select.value;
                    }
                });

                insurerFieldEl.insertAdjacentElement("afterend", built.field);
            })
            .catch(function () {
                // No product dropdown for this insurer. The productName text
                // input is unaffected either way.
            });
    }

    document.addEventListener("DOMContentLoaded", function () {
        var insurerNameInput = document.getElementById("insurerName");
        var productNameInput = document.getElementById("productName");
        if (!insurerNameInput || !productNameInput) {
            return;
        }

        var insurerFieldEl = insurerNameInput.closest(".field");
        if (!insurerFieldEl) {
            return;
        }

        fetchJson("/api/v1/insurers")
            .then(function (insurers) {
                if (!Array.isArray(insurers) || insurers.length === 0) {
                    return;
                }

                var built = buildField("Or choose an insurer from the list", "catalogueInsurerSelect");

                insurers.forEach(function (insurer) {
                    var option = document.createElement("option");
                    option.value = String(insurer.id);
                    option.textContent = insurer.displayName;
                    option.dataset.displayName = insurer.displayName;
                    built.select.appendChild(option);
                });

                built.select.addEventListener("change", function () {
                    removeExistingProductField();
                    if (built.select.value === "") {
                        return;
                    }
                    var selectedOption = built.select.options[built.select.selectedIndex];
                    insurerNameInput.value = selectedOption.dataset.displayName;
                    setupProductDropdown(built.select.value, built.field, productNameInput);
                });

                insurerFieldEl.insertAdjacentElement("afterend", built.field);
            })
            .catch(function () {
                // No insurer dropdown. The plain text fields work exactly as before.
            });
    });
})();

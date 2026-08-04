package com.antar.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GapControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String POLICY_JSON = """
            {
              "insurerName": "Sample Insurer",
              "productName": "Family Floater 5L",
              "sumInsured": 500000.00,
              "remainingSumInsured": 500000.00,
              "roomRentPercentOfSumInsuredPerDay": 0.0100,
              "proportionPharmacy": false,
              "coPayPercent": 0.1000,
              "coPayOrder": "AFTER_PROPORTION",
              "subLimits": [ { "procedureCode": "CATARACT", "cap": 40000.00 } ]
            }
            """;

    private static final String BILL_TEMPLATE = """
            {
              "policyId": %d,
              "procedureCode": "GENERAL_SURGERY",
              "daysAdmitted": 5,
              "actualRoomRentPerDay": 8000.00,
              "lines": [
                { "description": "Room charges",      "category": "ROOM",        "amount": 40000.00 },
                { "description": "Surgeon fees",      "category": "ASSOCIATED",  "amount": 95000.00 },
                { "description": "Nursing and OT",    "category": "ASSOCIATED",  "amount": 60000.00 },
                { "description": "Investigations",    "category": "ASSOCIATED",  "amount": 35000.00 },
                { "description": "Pharmacy",          "category": "PHARMACY",    "amount": 38000.00 },
                { "description": "Implants",          "category": "IMPLANT",     "amount": 14000.00 },
                { "description": "Non payable items", "category": "NON_PAYABLE", "amount": 18000.00 }
              ]
            }
            """;

    @Test
    void endToEnd_storePolicyThenComputeGap() throws Exception {
        String policyBody = mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(POLICY_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long policyId = objectMapper.readTree(policyBody).get("id").asLong();

        String gapBody = mockMvc.perform(post("/api/v1/gap/compute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BILL_TEMPLATE.formatted(policyId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode gap = objectMapper.readTree(gapBody);

        assertThat(gap.get("totalBill").asDouble()).isEqualTo(300000.00);
        assertThat(gap.get("payout").asDouble()).isEqualTo(176175.00);
        assertThat(gap.get("gap").asDouble()).isEqualTo(123825.00);
        assertThat(gap.get("deductions")).hasSize(4);
    }

    @Test
    void unknownPolicy_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/gap/compute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BILL_TEMPLATE.formatted(999999)))
                .andExpect(status().isNotFound());
    }

  @Test
    void invalidRequest_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/gap/compute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "policyId": 1, "daysAdmitted": 0, "lines": [] }
                                """))
                .andExpect(status().isBadRequest());
    }
}

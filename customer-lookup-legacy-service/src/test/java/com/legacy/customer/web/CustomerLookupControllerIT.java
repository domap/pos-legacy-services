package com.legacy.customer.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerLookupControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void lookupByEmail_ok() throws Exception {
        mockMvc.perform(get("/api/customer/lookup/email").param("email", "jane.doe@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.email").value("jane.doe@example.com"));
    }

    @Test
    void lookupByEmail_notFound() throws Exception {
        mockMvc.perform(get("/api/customer/lookup/email").param("email", "missing@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void lookupByTelephone_ok() throws Exception {
        mockMvc.perform(get("/api/customer/lookup/telephone").param("telephone", "555-0100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.customers", hasSize(1)));
    }

    @Test
    void lookupByTelephone_notFound() throws Exception {
        mockMvc.perform(get("/api/customer/lookup/telephone").param("telephone", "000-0000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void addCustomer_badRequestWhenEmailMissing() throws Exception {
        mockMvc.perform(post("/api/customer/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    void addAndUpdate_ok() throws Exception {
        MvcResult add = mockMvc.perform(post("/api/customer/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new.user@example.com","telephone":"555-9999","firstName":"New","lastName":"User"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.email").value("new.user@example.com"))
                .andReturn();

        JsonNode created = objectMapper.readTree(add.getResponse().getContentAsString());
        long id = created.get("id").asLong();

        mockMvc.perform(get("/api/customer/lookup/email").param("email", "new.user@example.com"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/customer/update/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    @Test
    void update_notFound() throws Exception {
        mockMvc.perform(put("/api/customer/update/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"X\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void actuatorReadiness_up() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void actuatorLiveness_up() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void actuatorPrometheus_exposed() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }
}

package com.storeops.alerts;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AlertsRoutesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void alertsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/alerts")).andExpect(status().isUnauthorized());
    }

    @Test
    void addingAMemberAndAssigningAnActivityRaiseAlertsForTheRecipientOnly() throws Exception {
        String programmeResponse = mockMvc.perform(post("/api/programmes")
                        .header("X-User-Id", "u-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alerts demo\"}"))
                .andReturn().getResponse().getContentAsString();
        String programmeId = JsonPath.read(programmeResponse, "$.id");

        mockMvc.perform(post("/api/programmes/" + programmeId + "/members")
                        .header("X-User-Id", "u-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"staffId\":\"u-2\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/activities")
                        .header("X-User-Id", "u-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Alert activity\",\"assigneeId\":\"u-2\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/alerts").header("X-User-Id", "u-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type == 'PROGRAMME_MEMBER_ADDED')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.type == 'ACTIVITY_ASSIGNED')]").isNotEmpty());
        mockMvc.perform(get("/api/alerts").header("X-User-Id", "u-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}

package com.storeops;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** End-to-end API tests. Assertions verify business outcomes, not only HTTP status codes. */
@SpringBootTest
@AutoConfigureMockMvc
class StoreOpsApiTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    private String createActivity(String staff, String json) throws Exception {
        String body = mvc.perform(post("/api/activities").header("X-Staff-Id", staff)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(body);
        return node.get("id").asText();
    }

    @Test
    void listActivitiesIsOpen() throws Exception {
        mvc.perform(get("/api/activities")).andExpect(status().isOk());
    }

    @Test
    void createThenGetReturnsSameActivityWithDefaults() throws Exception {
        String id = createActivity("s-1", "{\"title\":\"Restock aisle 4\"}");
        mvc.perform(get("/api/activities/" + id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.title", is("Restock aisle 4")))
            .andExpect(jsonPath("$.status", is("TODO")))
            .andExpect(jsonPath("$.ownerId", is("s-1")));
    }

    @Test
    void blankTitleIsRejectedWithTypedError() throws Exception {
        mvc.perform(post("/api/activities").header("X-Staff-Id", "s-1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void missingStaffHeaderIsBadRequest() throws Exception {
        mvc.perform(post("/api/activities").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void unknownActivityIsNotFoundWithTypedBody() throws Exception {
        mvc.perform(get("/api/activities/nope")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    @Test
    void invalidStatusFilterIsBadRequest() throws Exception {
        mvc.perform(get("/api/activities?status=NOPE")).andExpect(status().isBadRequest());
    }

    @Test
    void blockingAnActivityRaisesEscalationAlertForOwnerViaEventBus() throws Exception {
        String id = createActivity("s-1", "{\"title\":\"Planogram reset\",\"category\":\"PLANOGRAM\"}");
        mvc.perform(patch("/api/activities/" + id).header("X-Staff-Id", "s-3")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"BLOCKED\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("BLOCKED")));
        mvc.perform(get("/api/alerts").header("X-Staff-Id", "s-1")).andExpect(status().isOk())
            .andExpect(jsonPath("$[*].type", hasItem("ESCALATION")))
            .andExpect(jsonPath("$[*].message", hasItem("Activity " + id + " is BLOCKED")));
    }

    @Test
    void reopeningDoneActivityIsConflict() throws Exception {
        String id = createActivity("s-1", "{\"title\":\"Audit\"}");
        mvc.perform(patch("/api/activities/" + id).header("X-Staff-Id", "s-1")
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\"}")).andExpect(status().isOk());
        mvc.perform(patch("/api/activities/" + id).header("X-Staff-Id", "s-1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"TODO\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code", is("CONFLICT")));
    }

    @Test
    void deleteIsForbiddenForAssociateAndRemovesForManager() throws Exception {
        String id = createActivity("s-2", "{\"title\":\"Lead task\"}");
        mvc.perform(delete("/api/activities/" + id).header("X-Staff-Id", "s-3")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/activities/" + id).header("X-Staff-Id", "s-1")).andExpect(status().isNoContent());
        mvc.perform(get("/api/activities/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void programmeLifecycleCreateListAddMember() throws Exception {
        String body = mvc.perform(post("/api/programmes").header("X-Staff-Id", "s-1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Autumn refit\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(body).get("id").asText();
        mvc.perform(post("/api/programmes/" + id + "/members").header("X-Staff-Id", "s-1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"staffId\":\"s-3\",\"role\":\"ASSOCIATE\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.members", hasSize(1)))
            .andExpect(jsonPath("$.members[0].staffId", is("s-3")));
        mvc.perform(get("/api/programmes").header("X-Staff-Id", "s-2")).andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", hasItem("Autumn refit")));
        mvc.perform(post("/api/programmes/" + id + "/members").header("X-Staff-Id", "s-1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"staffId\":\"s-3\",\"role\":\"ASSOCIATE\"}"))
            .andExpect(status().isConflict());
    }
}

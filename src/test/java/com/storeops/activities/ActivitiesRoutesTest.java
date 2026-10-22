package com.storeops.activities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.storeops.activities.repository.ActivityAuditRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ActivitiesRoutesTest {

    private static final String MANAGER = "u-1";
    private static final String STAFF = "u-2";
    private static final String OTHER_STORE = "u-3";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ActivityAuditRepository auditRepository;

    private String createActivity(String userId, String json) throws Exception {
        String response = mockMvc.perform(post("/api/activities")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    @Test
    void createActivityAppliesDefaults() throws Exception {
        mockMvc.perform(post("/api/activities")
                        .header("X-User-Id", STAFF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Restock aisle 4\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.category").value("GENERAL"))
                .andExpect(jsonPath("$.ownerId").value(STAFF))
                .andExpect(jsonPath("$.storeId").value("store-1"));
    }

    @Test
    void createActivityWithBlankTitleReturns400() throws Exception {
        mockMvc.perform(post("/api/activities")
                        .header("X-User-Id", STAFF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/activities")
                        .header("X-User-Id", STAFF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void missingUserHeaderReturns401() throws Exception {
        mockMvc.perform(get("/api/activities"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void unknownUserReturns401() throws Exception {
        mockMvc.perform(get("/api/activities").header("X-User-Id", "u-999"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listFiltersByProgrammeAndStatus() throws Exception {
        String id = createActivity(STAFF, "{\"title\":\"A\",\"programmeId\":\"p-filter-1\"}");
        createActivity(STAFF, "{\"title\":\"B\",\"programmeId\":\"p-filter-2\"}");

        mockMvc.perform(get("/api/activities").param("programmeId", "p-filter-1").header("X-User-Id", STAFF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(patch("/api/activities/" + id)
                        .header("X-User-Id", STAFF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/activities")
                        .param("programmeId", "p-filter-1").param("status", "IN_PROGRESS")
                        .header("X-User-Id", STAFF))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/activities")
                        .param("programmeId", "p-filter-1").param("status", "DONE")
                        .header("X-User-Id", STAFF))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void invalidStatusFilterReturns400() throws Exception {
        mockMvc.perform(get("/api/activities").param("status", "NOPE").header("X-User-Id", STAFF))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getByIdReturnsActivityAndHidesOtherStores() throws Exception {
        String id = createActivity(STAFF, "{\"title\":\"Visible\"}");

        mockMvc.perform(get("/api/activities/" + id).header("X-User-Id", MANAGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Visible"));
        mockMvc.perform(get("/api/activities/" + id).header("X-User-Id", OTHER_STORE))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/activities/act-missing").header("X-User-Id", STAFF))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void patchUpdatesStatusPriorityCategoryAndAssignee() throws Exception {
        String id = createActivity(MANAGER, "{\"title\":\"Patch me\"}");

        mockMvc.perform(patch("/api/activities/" + id)
                        .header("X-User-Id", MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\",\"priority\":\"HIGH\","
                                + "\"category\":\"RESTOCKING\",\"assigneeId\":\"u-2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.category").value("RESTOCKING"))
                .andExpect(jsonPath("$.assigneeId").value("u-2"));
    }

    @Test
    void patchRejectsUnknownOrForeignAssignee() throws Exception {
        String id = createActivity(MANAGER, "{\"title\":\"Assign me\"}");

        mockMvc.perform(patch("/api/activities/" + id)
                        .header("X-User-Id", MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"u-999\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/activities/" + id)
                        .header("X-User-Id", MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"u-3\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownerCanDeleteOwnActivity() throws Exception {
        String id = createActivity(STAFF, "{\"title\":\"Delete me\"}");

        mockMvc.perform(delete("/api/activities/" + id).header("X-User-Id", STAFF))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/activities/" + id).header("X-User-Id", STAFF))
                .andExpect(status().isNotFound());
    }

    @Test
    void storeManagerCanDeleteAnyActivityInTheirStore() throws Exception {
        String id = createActivity(STAFF, "{\"title\":\"Manager deletes\"}");

        mockMvc.perform(delete("/api/activities/" + id).header("X-User-Id", MANAGER))
                .andExpect(status().isNoContent());
    }

    @Test
    void nonOwnerStaffCannotDelete() throws Exception {
        String id = createActivity(MANAGER, "{\"title\":\"Protected\"}");

        mockMvc.perform(delete("/api/activities/" + id).header("X-User-Id", STAFF))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void bulkStatusReturnsOrderedPartialResults() throws Exception {
        String first = createActivity(STAFF, "{\"title\":\"First\"}");
        String last = createActivity(STAFF, "{\"title\":\"Last\"}");

        mockMvc.perform(patch("/api/activities/bulk-status")
                        .header("X-User-Id", STAFF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityIds\":[\"" + first + "\",\"act-missing\",\""
                                + last + "\"],\"status\":\"DONE\"}"))
                .andExpect(status().isMultiStatus())
                .andExpect(jsonPath("$.results.length()").value(3))
                .andExpect(jsonPath("$.results[0].outcome").value("SUCCEEDED"))
                .andExpect(jsonPath("$.results[1].errorCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.results[2].outcome").value("SUCCEEDED"));
        mockMvc.perform(get("/api/activities/" + first).header("X-User-Id", STAFF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
        assertEquals(1, auditRepository.findByActivityId(first).size());
        assertEquals(0, auditRepository.findByActivityId("act-missing").size());
        assertEquals(1, auditRepository.findByActivityId(last).size());
    }

    @Test
    void bulkStatusValidatesBodyAndRequiresAuthentication() throws Exception {
        String id = createActivity(STAFF, "{\"title\":\"Unchanged\"}");
        mockMvc.perform(patch("/api/activities/bulk-status")
                        .header("X-User-Id", STAFF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityIds\":[\"" + id + "\"],\"status\":\"TODO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(patch("/api/activities/bulk-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityIds\":[\"" + id + "\"],\"status\":\"DONE\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(patch("/api/activities/bulk-status")
                        .header("X-User-Id", "u-999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityIds\":[\"" + id + "\"],\"status\":\"DONE\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/activities/" + id).header("X-User-Id", STAFF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TODO"));
        assertTrue(auditRepository.findByActivityId(id).isEmpty());
    }

    @Test
    void bulkStatusRejectsEmptyBlankAndMalformedRequests() throws Exception {
        String id = createActivity(STAFF, "{\"title\":\"Unchanged\"}");
        mockMvc.perform(patch("/api/activities/bulk-status")
                        .header("X-User-Id", STAFF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityIds\":[],\"status\":\"DONE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(patch("/api/activities/bulk-status")
                        .header("X-User-Id", STAFF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityIds\":[\" \"],\"status\":\"DONE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(patch("/api/activities/bulk-status")
                        .header("X-User-Id", STAFF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/activities/" + id).header("X-User-Id", STAFF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TODO"));
        assertTrue(auditRepository.findByActivityId(id).isEmpty());
    }

    @Test
    void bulkStatusAllSuccessReturnsOkAndForeignStoreIsNotFound() throws Exception {
        String own = createActivity(STAFF, "{\"title\":\"Own\"}");
        String foreign = createActivity(OTHER_STORE, "{\"title\":\"Foreign\"}");

        mockMvc.perform(patch("/api/activities/bulk-status")
                        .header("X-User-Id", STAFF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityIds\":[\"" + own + "\"],\"status\":\"BLOCKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].outcome").value("SUCCEEDED"));
        mockMvc.perform(patch("/api/activities/bulk-status")
                        .header("X-User-Id", STAFF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityIds\":[\"" + foreign + "\"],\"status\":\"DONE\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.results[0].errorCode").value("NOT_FOUND"));
    }
}

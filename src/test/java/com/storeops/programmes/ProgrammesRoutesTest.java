package com.storeops.programmes;

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
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ProgrammesRoutesTest {

    @Autowired
    private MockMvc mockMvc;

    private String createProgramme(String userId, String name) throws Exception {
        String response = mockMvc.perform(post("/api/programmes")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"description\":\"d\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.members.length()").value(0))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private ResultActions addMember(String userId, String programmeId,
                                String staffId) throws Exception {
        return mockMvc.perform(post("/api/programmes/" + programmeId + "/members")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"staffId\":\"" + staffId + "\"}"));
    }

    @Test
    void createWithBlankNameReturns400() throws Exception {
        mockMvc.perform(post("/api/programmes")
                        .header("X-User-Id", "u-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listReturnsOnlyProgrammesOfTheCallersStore() throws Exception {
        createProgramme("u-1", "Store One Only");

        mockMvc.perform(get("/api/programmes").header("X-User-Id", "u-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Store One Only')]").isNotEmpty());
        mockMvc.perform(get("/api/programmes").header("X-User-Id", "u-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Store One Only')]").isEmpty());
    }

    @Test
    void addMemberAddsStaffAndRejectsDuplicates() throws Exception {
        String id = createProgramme("u-1", "Onboarding");

        addMember("u-1", id, "u-2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[0].staffId").value("u-2"))
                .andExpect(jsonPath("$.members[0].role").value("ASSOCIATE"));
        addMember("u-1", id, "u-2")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void addMemberValidatesProgrammeAndStaff() throws Exception {
        String id = createProgramme("u-1", "Validation");

        addMember("u-1", "prg-missing", "u-2").andExpect(status().isNotFound());
        addMember("u-1", id, "u-999").andExpect(status().isNotFound());
        addMember("u-1", id, "u-3").andExpect(status().isBadRequest());
        addMember("u-3", id, "u-3").andExpect(status().isNotFound());
        mockMvc.perform(post("/api/programmes/" + id + "/members")
                        .header("X-User-Id", "u-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"staffId\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}

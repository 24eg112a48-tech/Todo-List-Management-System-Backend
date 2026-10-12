package com.example.todo;

import com.example.todo.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerTest {

    private static final String BASE = "/api/v1/tasks";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TaskRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void crudFlow() throws Exception {
        // create
        String created = mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Task one\",\"description\":\"hello\",\"priority\":\"HIGH\",\"status\":\"PENDING\",\"dueDate\":\"2030-01-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Task one"))
                .andReturn().getResponse().getContentAsString();

        long id = ((Number) com.jayway.jsonpath.JsonPath.read(created, "$.id")).longValue();

        // get by id
        mvc.perform(get(BASE + "/" + id)).andExpect(status().isOk());

        // list with search + filter
        mvc.perform(get(BASE).param("search", "one").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // update
        mvc.perform(put(BASE + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Task one updated\",\"priority\":\"LOW\",\"status\":\"PENDING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Task one updated"));

        // complete
        mvc.perform(patch(BASE + "/" + id + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // reopen
        mvc.perform(patch(BASE + "/" + id + "/reopen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // complete again, then stats
        mvc.perform(patch(BASE + "/" + id + "/complete")).andExpect(status().isOk());
        mvc.perform(get(BASE + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.completed").value(1))
                .andExpect(jsonPath("$.completionPercentage").value(100.0));

        // delete
        mvc.perform(delete(BASE + "/" + id)).andExpect(status().isNoContent());
        mvc.perform(get(BASE + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void createWithoutTitleIsBadRequest() throws Exception {
        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"priority\":\"MEDIUM\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidEnumValueIsBadRequest() throws Exception {
        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bad\",\"priority\":\"URGENT\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingIdReturns404() throws Exception {
        mvc.perform(get(BASE + "/999999")).andExpect(status().isNotFound());
        mvc.perform(delete(BASE + "/999999")).andExpect(status().isNotFound());
        mvc.perform(patch(BASE + "/999999/complete")).andExpect(status().isNotFound());
    }

    @Test
    void overdueFilterWorks() throws Exception {
        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Overdue one\",\"priority\":\"MEDIUM\",\"status\":\"PENDING\",\"dueDate\":\"2000-01-01\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get(BASE).param("overdue", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void healthEndpointReportsUp() throws Exception {
        mvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void statusFilterWorks() throws Exception {
        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Done one\",\"status\":\"COMPLETED\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Open one\",\"status\":\"PENDING\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get(BASE).param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}

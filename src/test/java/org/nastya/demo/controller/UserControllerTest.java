package org.nastya.demo.controller;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.nastya.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Test
    void getAllUsers_noTokenIs401() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_noTokenIs200() throws Exception {
        String token = loginAndGetAdminToken();
        mockMvc.perform(get("/users")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createUser_noTokenIs200() throws Exception {
        String token = loginAndGetAdminToken();
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token))
                        .content("""
                                {
                                  "username": "newUser",
                                  "password": "12345",
                                  "role": "USER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.notNullValue()));
    }

    @Test
    void updateUser_withAdminToken_returnsUpdatedUser() throws Exception {
        String token = loginAndGetAdminToken();
        UUID userId = userRepository.getUserIdByUsername("user3");

        mockMvc.perform(put("/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token))
                        .content("""
                                {
                                  "username": "user99",
                                  "password": "12345",
                                  "role": "USER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user99"));
    }

    @Test
    void deleteUser_withAdminToken_returnsOk() throws Exception {
        String token = loginAndGetAdminToken();
        UUID userId = userRepository.getUserIdByUsername("user4");

        mockMvc.perform(delete("/users/" + userId)
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isOk());
    }

    @Test
    void getUserById_withUserToken_returnsForbidden() throws Exception {
        String token = loginAndGetUserToken();
        UUID userId = userRepository.getUserIdByUsername("user5");

        mockMvc.perform(get("/users/" + userId)
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_asRegularUser_forbidden() throws Exception {
        String token = loginAndGetUserToken();
        mockMvc.perform(get("/users")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_asRegularUser_forbidden() throws Exception {
        String token = loginAndGetUserToken();
        UUID userId = userRepository.getUserIdByUsername("user5");

        mockMvc.perform(get("/users/" + userId)
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_asRegularUser_forbidden() throws Exception {
        String token = loginAndGetUserToken();
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token))
                        .content("""
                                {
                                  "username": "newUser",
                                  "password": "12345",
                                  "role": "USER"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_asRegularUser_forbidden() throws Exception {
        String token = loginAndGetUserToken();
        UUID userId = userRepository.getUserIdByUsername("user5");

        mockMvc.perform(put("/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token))
                        .content("""
                                {
                                  "username": "updatedUser",
                                  "password": "12345",
                                  "role": "USER"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_asRegularUser_forbidden() throws Exception {
        String token = loginAndGetUserToken();
        UUID userId = userRepository.getUserIdByUsername("user5");

        mockMvc.perform(delete("/users/{id}", userId)
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isForbidden());
    }

    private String loginAndGetAdminToken() throws Exception {
        return loginAndGetToken("user1");
    }

    private String loginAndGetUserToken() throws Exception {
        return loginAndGetToken("user3");
    }

    private String loginAndGetToken(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "12345"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private String getAuthHeader(String token) {
        return "Bearer " + token;
    }
}
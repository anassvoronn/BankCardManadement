package org.nastya.demo.controller;

import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.ServletException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.nastya.demo.entity.User;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
class UserControllerTest {

    public static final String INVALID_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMSIsImlhdCI6MTc2Nzk1NjgzNCwiZXhwIjoxNzY3OTYwNDM0fQ.NK_Wt8E6Np75IQbJLvxeAgwZvsYHDeY-uUKlvjOtiS4";
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
    void getAllUsers_noTokenIs403() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_noTokenIs200() throws Exception {
        String token = loginAndGetAdminToken();
        mockMvc.perform(get("/users")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isOk());
    }

    @Test
    void getAllUsers_BadToken() {
        assertThrows(SignatureException.class, () ->
                mockMvc.perform(get("/users")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(INVALID_TOKEN)))
        );
    }

    @Test
    void getAllUsers_asRegularUser_forbidden() throws Exception {
        String token = loginAndGetUserToken();
        mockMvc.perform(get("/users")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_withAdminToken_returnsOk() throws Exception {
        String token = loginAndGetAdminToken();
        UUID userId = userRepository.getUserIdByUsername("user6");

        mockMvc.perform(get("/users/" + userId)
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isOk());
    }

    @Test
    void getUserById_BadToken() {
        UUID userId = userRepository.getUserIdByUsername("user6");

        assertThrows(SignatureException.class, () ->
                mockMvc.perform(get("/users/" + userId)
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(INVALID_TOKEN)))
        );
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
    void getUserById_noToken() throws Exception {
        UUID userId = userRepository.getUserIdByUsername("user5");

        mockMvc.perform(get("/users/" + userId))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_noToken() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "newUser",
                                  "password": "12345",
                                  "role": "USER"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().string(Matchers.notNullValue()));
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
    void createUser_BadToken() {
        assertThrows(SignatureException.class, () ->
                mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(INVALID_TOKEN))
                        .content("""
                                {
                                  "username": "newUser",
                                  "password": "12345",
                                  "role": "USER"
                                }
                                """))
        );
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
    void updateUser_withAdminToken_returnsUpdatedUser() throws Exception {
        String token = loginAndGetAdminToken();
        UUID userId = userRepository.getUserIdByUsername("user5");

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
    void updateUser_BadToken() {
        UUID userId = userRepository.getUserIdByUsername("user5");

        assertThrows(SignatureException.class, () ->
                mockMvc.perform(put("/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(INVALID_TOKEN))
                        .content("""
                                {
                                  "username": "user99",
                                  "password": "12345",
                                  "role": "USER"
                                }
                                """))
        );
    }

    @Test
    void updateUser_noToken() throws Exception {
        UUID userId = userRepository.getUserIdByUsername("user5");
        mockMvc.perform(put("/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "user99",
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
    void deleteUser_withAdminToken_returnsServerError() throws Exception {
        String token = loginAndGetAdminToken();
        UUID userId = userRepository.getUserIdByUsername("user4");

        assertThrows(ServletException.class, () ->
                mockMvc.perform(delete("/users/" + userId)
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
        );

        Optional<User> deletedUser = userRepository.findById(userId);
        assertFalse(deletedUser.isEmpty());
    }

    @Test
    void deleteUser_withAdminToken() throws Exception {
        String token = loginAndGetAdminToken();
        UUID userId = userRepository.getUserIdByUsername("user11");

        mockMvc.perform(delete("/users/" + userId)
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isOk());

        Optional<User> deletedUser = userRepository.findById(userId);
        assertTrue(deletedUser.isEmpty());

    }

    @Test
    void deleteUser_BadToken() {
        UUID userId = userRepository.getUserIdByUsername("user4");

        assertThrows(SignatureException.class, () ->
                mockMvc.perform(delete("/users/" + userId)
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(INVALID_TOKEN)))
        );
    }

    @Test
    void deleteUser_noToken_returnsServerError() throws Exception {
        UUID userId = userRepository.getUserIdByUsername("user4");

        mockMvc.perform(delete("/users/" + userId))
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
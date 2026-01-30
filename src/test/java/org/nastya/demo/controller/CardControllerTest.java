package org.nastya.demo.controller;

import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.nastya.demo.entity.Card;
import org.nastya.demo.entity.User;
import org.nastya.demo.repository.CardRepository;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
class CardControllerTest {

    public static final String INVALID_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMSIsImlhdCI6MTc2Nzk1NjgzNCwiZXhwIjoxNzY3OTYwNDM0fQ.NK_Wt8E6Np75IQbJLvxeAgwZvsYHDeY-uUKlvjOtiS4";
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardRepository cardRepository;

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
    void getAllCards_admin200() throws Exception {
        String token = loginAndGetAdminToken();
        mockMvc.perform(get("/cards")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isOk());
    }

    @Test
    void getAllCards_user403() throws Exception {
        String token = loginAndGetUser3Token();
        mockMvc.perform(get("/cards")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllCards_noToken403() throws Exception {
        mockMvc.perform(get("/cards"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllCards_invalidToken403() {
        assertThrows(SignatureException.class, () ->
                mockMvc.perform(get("/cards")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(INVALID_TOKEN)))
        );
    }

    @Test
    void getCardById_admin200() throws Exception {
        String token = loginAndGetAdminToken();

        Card card = cardRepository.findAll().get(0);
        mockMvc.perform(get("/cards/" + card.getId())
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isOk());
    }

    @Test
    void getCardById_user403() throws Exception {
        String token = loginAndGetUser3Token();

        Card card = cardRepository.findAll().get(0);
        mockMvc.perform(get("/cards/" + card.getId())
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCardById_noToken403() throws Exception {
        Card card = cardRepository.findAll().get(0);
        mockMvc.perform(get("/cards/" + card.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCardById_invalidToken403() {
        Card card = cardRepository.findAll().get(0);
        assertThrows(SignatureException.class, () ->
                mockMvc.perform(get("/cards/" + card.getId())
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(INVALID_TOKEN)))
        );
    }

    @Test
    void getMyCards_user200() throws Exception {
        String token = loginAndGetUser3Token();
        mockMvc.perform(get("/cards/private")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isOk());
    }

    @Test
    void getMyCards_admin403() throws Exception {
        String token = loginAndGetAdminToken();
        mockMvc.perform(get("/cards/private")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyCards_noToken403() throws Exception {
        mockMvc.perform(get("/cards/private"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyCards_invalidToken403() {
        assertThrows(SignatureException.class, () ->
                mockMvc.perform(get("/cards/private")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(INVALID_TOKEN)))
        );
    }

    @Test
    void getMyCardById_user200() throws Exception {
        String token = loginAndGetUser3Token();

        Card card = cardRepository.findByUserUsername("user3").get(0);
        mockMvc.perform(get("/cards/private/" + card.getId())
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isOk());
    }

    @Test
    void getMyCardById_wrongCardId() throws Exception {
        String token = loginAndGetUser3Token();

        Card card = cardRepository.findByUserUsername("user4").get(0);
        assertThrows(ServletException.class, () ->
                mockMvc.perform(get("/cards/private/" + card.getId())
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
        );
    }

    @Test
    void getMyCardById_admin403() throws Exception {
        String token = loginAndGetAdminToken();

        Card card = cardRepository.findByUserUsername("user3").get(0);
        mockMvc.perform(get("/cards/private/" + card.getId())
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyCardById_noToken403() throws Exception {
        Card card = cardRepository.findByUserUsername("user3").get(0);
        mockMvc.perform(get("/cards/private/" + card.getId()))
                .andExpect(status().isForbidden());
    }


    @Test
    void getMyCardById_invalidToken401() {
        Card card = cardRepository.findByUserUsername("user3").get(0);

        assertThrows(SignatureException.class, () ->
                mockMvc.perform(get("/cards/private/" + card.getId())
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(INVALID_TOKEN)))
        );
    }

    @Test
    void createCard_admin200() throws Exception {
        String token = loginAndGetAdminToken();

        User user = userRepository.findByUsername("user6").orElseThrow();
        mockMvc.perform(post("/cards")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardNumber": "1111222233334444",
                                  "ownerName": "user6",
                                  "expiryDate": "2030-01-01",
                                  "userId": "%s"
                                }
                                """.formatted(user.getId())))
                .andExpect(status().isOk());
    }

    @Test
    void createCard_user403() throws Exception {
        String token = loginAndGetUser6Token();

        User user = userRepository.findByUsername("user6").orElseThrow();
        mockMvc.perform(post("/cards")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardNumber": "1111222233334444",
                                  "ownerName": "user6",
                                  "expiryDate": "2030-01-01",
                                  "userId": "%s"
                                }
                                """.formatted(user.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCard_noToken403() throws Exception {
        mockMvc.perform(post("/cards"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCard_invalidToken403() {
        assertThrows(SignatureException.class, () ->
                mockMvc.perform(post("/cards")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(INVALID_TOKEN)))
        );
    }

    @Test
    void updateCard_admin200() throws Exception {
        String token = loginAndGetAdminToken();

        Card card = cardRepository.findAll().get(0);
        mockMvc.perform(put("/cards/" + card.getId())
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardNumber": "9999000011112222",
                                  "ownerName": "user5",
                                  "expiryDate": "2030-01-01",
                                  "userId": "%s"
                                }
                                """.formatted(card.getUser().getId())))
                .andExpect(status().isOk());
    }

    @Test
    void updateCard_user403() throws Exception {
        String token = loginAndGetUser6Token();

        Card card = cardRepository.findAll().get(0);
        mockMvc.perform(put("/cards/" + card.getId())
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardNumber": "8888777766665555",
                                  "ownerName": "user3",
                                  "expiryDate": "2030-12-31",
                                  "userId": "%s"
                                }
                                """.formatted(card.getUser().getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCard_noToken403() throws Exception {
        Card card = cardRepository.findAll().get(0);
        mockMvc.perform(put("/cards/" + card.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCard_invalidToken403() {
        Card card = cardRepository.findAll().get(0);

        assertThrows(SignatureException.class, () ->
                mockMvc.perform(put("/cards/" + card.getId())
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(INVALID_TOKEN)))
        );
    }

    @Test
    void deleteCard_admin200() throws Exception {
        String token = loginAndGetAdminToken();

        Card card = cardRepository.findAll().get(0);
        mockMvc.perform(delete("/cards/" + card.getId())
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteCard_user403() throws Exception {
        String token = loginAndGetUser3Token();

        Card card = cardRepository.findAll().get(0);
        mockMvc.perform(delete("/cards/" + card.getId())
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCard_noToken403() throws Exception {
        Card card = cardRepository.findAll().get(0);
        mockMvc.perform(delete("/cards/" + card.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCard_invalidToken401() {
        Card card = cardRepository.findAll().get(0);

        assertThrows(SignatureException.class, () ->
                mockMvc.perform(delete("/cards/" + card.getId())
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(INVALID_TOKEN)))
        );
    }

    @Test
    void transferBetweenOwnCards_user200() throws Exception {
        String token = loginAndGetUser3Token();

        List<Card> cards = cardRepository.findByUserUsername("user3");
        mockMvc.perform(put("/cards/transfer")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromCardId": "%s",
                                  "toCardId": "%s",
                                  "amount": 100
                                }
                                """.formatted(cards.get(0).getId(), cards.get(1).getId())))
                .andExpect(status().isOk());
    }

    @Test
    void transferBetweenOwnCards_admin403() throws Exception {
        String token = loginAndGetAdminToken();

        List<Card> cards = cardRepository.findByUserUsername("user3");
        mockMvc.perform(put("/cards/transfer")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromCardId": "%s",
                                  "toCardId": "%s",
                                  "amount": 100
                                }
                                """.formatted(cards.get(0).getId(), cards.get(1).getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void transferBetweenOwnCards_noToken403() throws Exception {
        mockMvc.perform(put("/cards/transfer"))
                .andExpect(status().isForbidden());
    }

    @Test
    void transferBetweenOwnCards_invalidToken403() {
        assertThrows(SignatureException.class, () ->
                mockMvc.perform(put("/cards/transfer")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(INVALID_TOKEN)))
        );
    }

    @Test
    void changeCardStatus_admin200() throws Exception {
        String token = loginAndGetAdminToken();

        Card card = cardRepository.findByUserUsername("user6").get(0);

        mockMvc.perform(put("/cards/" + card.getId() + "/status")
                        .param("status", "BLOCKED")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isOk());
    }

    @Test
    void changeCardStatus_user403() throws Exception {
        String token = loginAndGetUser6Token();

        Card card = cardRepository.findByUserUsername("user6").get(0);

        mockMvc.perform(put("/cards/" + card.getId() + "/status")
                        .param("status", "BLOCKED")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void changeCardStatus_noToken401() throws Exception {
        Card card = cardRepository.findByUserUsername("user6").get(0);

        mockMvc.perform(put("/cards/" + card.getId() + "/status")
                        .param("status", "BLOCKED"))
                .andExpect(status().isForbidden());
    }

    @Test
    void changeCardStatus_invalidToken403() {
        Card card = cardRepository.findByUserUsername("user6").get(0);

        assertThrows(SignatureException.class, () ->
                mockMvc.perform(put("/cards/" + card.getId() + "/status")
                        .param("status", "BLOCKED")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(INVALID_TOKEN)))
        );
    }

    @Test
    void changeCardStatus_invalidStatus400() throws Exception {
        String token = loginAndGetAdminToken();

        Card card = cardRepository.findByUserUsername("user6").get(0);

        mockMvc.perform(put("/cards/" + card.getId() + "/status")
                        .param("status", "WRONG")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isBadRequest());
    }


    @Test
    void blockOwnCard_user200() throws Exception {
        String token = loginAndGetUser6Token();

        Card card = cardRepository.findByUserUsername("user6").get(0);
        mockMvc.perform(put("/cards/private/" + card.getId() + "/block")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isOk());
    }

    @Test
    void blockOwnCard_wrongCard() throws Exception {
        String token = loginAndGetUser3Token();

        Card card = cardRepository.findByUserUsername("user4").get(0);
        assertThrows(ServletException.class, () ->
                mockMvc.perform(put("/cards/private/" + card.getId() + "/block")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
        );
    }

    @Test
    void blockOwnCard_admin403() throws Exception {
        String token = loginAndGetAdminToken();

        Card card = cardRepository.findByUserUsername("user6").get(0);
        mockMvc.perform(put("/cards/private/" + card.getId() + "/block")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void blockOwnCard_noToken401() throws Exception {
        Card card = cardRepository.findByUserUsername("user6").get(0);
        mockMvc.perform(put("/cards/private/" + card.getId() + "/block"))
                .andExpect(status().isForbidden());
    }

    @Test
    void blockOwnCard_invalidToken403() {
        Card card = cardRepository.findByUserUsername("user6").get(0);

        assertThrows(SignatureException.class, () ->
                mockMvc.perform(put("/cards/private/" + card.getId() + "/block")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(INVALID_TOKEN)))
        );
    }

    @Test
    void getCardBalance_user200() throws Exception {
        String token = loginAndGetUser3Token();

        Card card = cardRepository.findByUserUsername("user3").get(1);
        mockMvc.perform(get("/cards/private/" + card.getId() + "/balance")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isOk());
    }

    @Test
    void getCardBalance_wrongCard() throws Exception {
        String token = loginAndGetUser3Token();

        Card card = cardRepository.findByUserUsername("user4").get(0);
        assertThrows(ServletException.class, () ->
                mockMvc.perform(get("/cards/private/" + card.getId() + "/balance")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
        );
    }

    @Test
    void getCardBalance_admin403() throws Exception {
        String token = loginAndGetAdminToken();

        Card card = cardRepository.findByUserUsername("user3").get(0);
        mockMvc.perform(get("/cards/private/" + card.getId() + "/balance")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCardBalance_noToken403() throws Exception {
        Card card = cardRepository.findByUserUsername("user3").get(0);
        mockMvc.perform(get("/cards/private/" + card.getId() + "/balance"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCardBalance_invalidToken403() {
        Card card = cardRepository.findByUserUsername("user3").get(0);

        assertThrows(SignatureException.class, () ->
                mockMvc.perform(get("/cards/private/" + card.getId() + "/balance")
                        .header(HttpHeaders.AUTHORIZATION, getAuthHeader(INVALID_TOKEN)))
        );
    }

    private String loginAndGetAdminToken() throws Exception {
        return loginAndGetToken("user1");
    }

    private String loginAndGetUser3Token() throws Exception {
        return loginAndGetToken("user3");
    }

    private String loginAndGetUser6Token() throws Exception {
        return loginAndGetToken("user6");
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
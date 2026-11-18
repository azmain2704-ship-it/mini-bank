package com.minibank;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

/** Goes through the real API (controllers, services, database) for every rule. Each test makes its own accounts. */
@SpringBootTest
@AutoConfigureMockMvc
class BankApiTest {

    @Autowired
    MockMvc mvc;

    // ---- accounts ----------------------------------------------------------------------------------

    @Test
    void opensAnAccountWithAnOpeningBalance() throws Exception {
        mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerName\":\"Fred\",\"openingBalance\":100.50}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/accounts/")))
                .andExpect(jsonPath("$.ownerName").value("Fred"))
                .andExpect(jsonPath("$.balance").value(100.50));
    }

    @Test
    void refusesNegativeOpeningBalance() throws Exception {
        mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerName\":\"Fred\",\"openingBalance\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("openingBalance")));
    }

    @Test
    void refusesMissingName() throws Exception {
        mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerName\":\"  \",\"openingBalance\":5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownAccountIs404() throws Exception {
        mvc.perform(get("/api/accounts/999999")).andExpect(status().isNotFound());
    }

    @Test
    void depositAddsMoney() throws Exception {
        long id = openAccount("Fred", "10.00");
        mvc.perform(post("/api/accounts/" + id + "/deposits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":2.55}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(12.55));
    }

    @Test
    void depositMustBePositive() throws Exception {
        long id = openAccount("Fred", "10.00");
        mvc.perform(post("/api/accounts/" + id + "/deposits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":0}"))
                .andExpect(status().isBadRequest());
        assertBalance(id, 10.00);
    }

    // ---- transfers ---------------------------------------------------------------------------------

    @Test
    void transferMovesMoneyBetweenAccounts() throws Exception {
        long a = openAccount("Alice", "100.00");
        long b = openAccount("Bob", "20.00");
        transfer(a, b, "30.25", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(30.25))
                .andExpect(jsonPath("$.fraudFlags", empty()));
        assertBalance(a, 69.75);
        assertBalance(b, 50.25);
    }

    @Test
    void cantSendMoreThanTheBalance() throws Exception {
        long a = openAccount("Alice", "50.00");
        long b = openAccount("Bob", "0.00");
        transfer(a, b, "50.01", null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value(containsString("Insufficient funds")));
        assertBalance(a, 50.00);
        assertBalance(b, 0.00);
    }

    @Test
    void canSendTheWholeBalance() throws Exception {
        long a = openAccount("Alice", "50.00");
        long b = openAccount("Bob", "0.00");
        transfer(a, b, "50.00", null).andExpect(status().isCreated());
        assertBalance(a, 0.00);
    }

    @Test
    void negativeAndZeroAmountsAreRefused() throws Exception {
        long a = openAccount("Alice", "50.00");
        long b = openAccount("Bob", "0.00");
        transfer(a, b, "-10.00", null).andExpect(status().isBadRequest());
        transfer(a, b, "0", null).andExpect(status().isBadRequest());
        assertBalance(a, 50.00);
    }

    @Test
    void fractionsOfAPennyAreRefused() throws Exception {
        long a = openAccount("Alice", "50.00");
        long b = openAccount("Bob", "0.00");
        transfer(a, b, "1.005", null).andExpect(status().isBadRequest());
    }

    @Test
    void cantTransferToTheSameAccount() throws Exception {
        long a = openAccount("Alice", "50.00");
        transfer(a, a, "1.00", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("same account")));
    }

    @Test
    void transferToUnknownAccountIs404() throws Exception {
        long a = openAccount("Alice", "50.00");
        transfer(a, 999999, "1.00", null).andExpect(status().isNotFound());
        assertBalance(a, 50.00);
    }

    // ---- history -----------------------------------------------------------------------------------

    @Test
    void everyTransferIsRecordedIncludingRefusedOnes() throws Exception {
        long a = openAccount("Alice", "10.00");
        long b = openAccount("Bob", "0.00");
        transfer(a, b, "4.00", null).andExpect(status().isCreated());
        transfer(a, b, "100.00", null).andExpect(status().isUnprocessableEntity());

        mvc.perform(get("/api/accounts/" + a + "/transfers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].status").value("REJECTED"))   // newest first
                .andExpect(jsonPath("$[1].status").value("COMPLETED"));
        // Shows up for the receiver too
        mvc.perform(get("/api/transfers").param("accountId", String.valueOf(b)))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // ---- fraud flags -------------------------------------------------------------------------------

    @Test
    void largeTransferIsFlaggedButStillGoesThrough() throws Exception {
        long a = openAccount("Alice", "20000.00");
        long b = openAccount("Bob", "0.00");
        long id = idOf(transfer(a, b, "5000.00", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fraudFlags", hasItem("LARGE_AMOUNT"))));
        assertBalance(b, 5000.00);

        mvc.perform(get("/api/fraud-flags"))
                .andExpect(jsonPath("$[?(@.transferId == " + id + ")].reason", hasItem("LARGE_AMOUNT")));
    }

    @Test
    void justUnderTheLimitIsNotFlagged() throws Exception {
        long a = openAccount("Alice", "20000.00");
        long b = openAccount("Bob", "0.00");
        transfer(a, b, "4999.99", null).andExpect(jsonPath("$.fraudFlags", empty()));
    }

    @Test
    void fifthTransferInAMinuteIsFlagged() throws Exception {
        long a = openAccount("Alice", "100.00");
        long b = openAccount("Bob", "0.00");
        for (int i = 1; i <= 4; i++) {
            transfer(a, b, "1.00", null).andExpect(jsonPath("$.fraudFlags", not(hasItem("RAPID_TRANSFERS"))));
        }
        transfer(a, b, "1.00", null).andExpect(jsonPath("$.fraudFlags", hasItem("RAPID_TRANSFERS")));
    }

    @Test
    void refusedTransfersDontCountTowardsTheRapidRule() throws Exception {
        long a = openAccount("Alice", "4.00");
        long b = openAccount("Bob", "0.00");
        for (int i = 0; i < 4; i++) {
            transfer(a, b, "1.00", null).andExpect(status().isCreated());
        }
        transfer(a, b, "1.00", null).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fraudFlags", empty()));
    }

    // ---- idempotency -------------------------------------------------------------------------------

    @Test
    void retryingWithTheSameKeyDoesntMoveMoneyTwice() throws Exception {
        long a = openAccount("Alice", "100.00");
        long b = openAccount("Bob", "0.00");
        String key = "test-" + System.nanoTime();
        long first = idOf(transfer(a, b, "10.00", key).andExpect(status().isCreated()));
        long second = idOf(transfer(a, b, "10.00", key)
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true")));
        org.assertj.core.api.Assertions.assertThat(second).isEqualTo(first);
        assertBalance(a, 90.00);
        assertBalance(b, 10.00);
    }

    @Test
    void reusingAKeyForADifferentTransferIsRefused() throws Exception {
        long a = openAccount("Alice", "100.00");
        long b = openAccount("Bob", "0.00");
        long c = openAccount("Carol", "0.00");
        String key = "test-" + System.nanoTime();
        transfer(a, b, "10.00", key).andExpect(status().isCreated());

        // different amount, then a different recipient: both refused, and no money moves
        transfer(a, b, "99.00", key).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Idempotency-Key reused"));
        transfer(a, c, "10.00", key).andExpect(status().isUnprocessableEntity());

        assertBalance(a, 90.00);
        assertBalance(b, 10.00);
        assertBalance(c, 0.00);
    }

    // ---- helpers -----------------------------------------------------------------------------------

    private long openAccount(String owner, String balance) throws Exception {
        String body = mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerName\":\"" + owner + "\",\"openingBalance\":" + balance + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions transfer(long from, long to, String amount, String key) throws Exception {
        var request = post("/api/transfers").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":" + from + ",\"toAccountId\":" + to + ",\"amount\":" + amount + "}");
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        return mvc.perform(request);
    }

    private static long idOf(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private void assertBalance(long accountId, double expected) throws Exception {
        mvc.perform(get("/api/accounts/" + accountId)).andExpect(jsonPath("$.balance").value(expected));
    }
}

package com.hesap.wallet.exception;

import com.hesap.wallet.controller.AccountController;
import com.hesap.wallet.controller.TransferController;
import com.hesap.wallet.service.AccountService;
import com.hesap.wallet.service.DepositService;
import com.hesap.wallet.service.transfer.AbstractTransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the global exception handler maps each error type to the correct HTTP status
 * and the standardized {@code { "errorCode", "message" }} JSON (with camelCase errorCode).
 */
@WebMvcTest(controllers = {AccountController.class, TransferController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private DepositService depositService;

    @MockitoBean
    private AbstractTransferService transferService;

    @Test
    void accountNotFound_returns404() throws Exception {
        when(depositService.deposit(eq(999L), any())).thenThrow(new AccountNotFoundException(999L));

        mockMvc.perform(post("/api/v1/accounts/999/deposit")
                        .contentType(APPLICATION_JSON)
                        .content("{\"amount\": 10.00}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void insufficientBalance_returns422() throws Exception {
        when(transferService.transfer(any())).thenThrow(new InsufficientBalanceException(1L));

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(APPLICATION_JSON)
                        .content("{\"sender_account_id\":1,\"receiver_account_id\":2,\"amount\":50.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_BALANCE"));
    }

    @Test
    void invalidTransfer_returns400() throws Exception {
        when(transferService.transfer(any()))
                .thenThrow(new InvalidTransferException("sender and receiver accounts must be different"));

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(APPLICATION_JSON)
                        .content("{\"sender_account_id\":1,\"receiver_account_id\":1,\"amount\":50.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TRANSFER"))
                .andExpect(jsonPath("$.message").value("sender and receiver accounts must be different"));
    }

    @Test
    void beanValidationFailure_returns400ValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/1/deposit")
                        .contentType(APPLICATION_JSON)
                        .content("{}")) // missing amount -> @NotNull
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void malformedBody_invalidCurrencyEnum_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(APPLICATION_JSON)
                        .content("{\"user_id\":1,\"currency\":\"GBP\"}")) // unknown currency
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
    }

    @Test
    void unexpectedException_returns500InternalError() throws Exception {
        when(transferService.transfer(any())).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(APPLICATION_JSON)
                        .content("{\"sender_account_id\":1,\"receiver_account_id\":2,\"amount\":50.00}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
    }
}

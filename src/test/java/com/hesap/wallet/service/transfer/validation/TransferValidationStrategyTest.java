package com.hesap.wallet.service.transfer.validation;

import com.hesap.wallet.entity.Account;
import com.hesap.wallet.enums.Currency;
import com.hesap.wallet.exception.InsufficientBalanceException;
import com.hesap.wallet.exception.InvalidTransferException;
import com.hesap.wallet.service.transfer.TransferContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferValidationStrategyTest {

    private static TransferContext request(Long sender, Long receiver, String amount) {
        return new TransferContext(sender, receiver, new BigDecimal(amount));
    }

    private static TransferContext locked(Currency senderCcy, String senderBalance,
                                          Currency receiverCcy, String amount) {
        TransferContext ctx = new TransferContext(1L, 2L, new BigDecimal(amount));
        ctx.setSender(Account.builder().id(1L).currency(senderCcy).balance(new BigDecimal(senderBalance)).build());
        ctx.setReceiver(Account.builder().id(2L).currency(receiverCcy).balance(BigDecimal.ZERO).build());
        return ctx;
    }

    @Test
    void positiveAmount_rejectsZeroAndNegative_acceptsPositive() {
        PositiveAmountValidator validator = new PositiveAmountValidator();
        assertThat(validator.phase()).isEqualTo(ValidationPhase.PRE_LOCK);

        assertThatThrownBy(() -> validator.validate(request(1L, 2L, "0")))
                .isInstanceOf(InvalidTransferException.class);
        assertThatThrownBy(() -> validator.validate(request(1L, 2L, "-5.00")))
                .isInstanceOf(InvalidTransferException.class);
        assertThatCode(() -> validator.validate(request(1L, 2L, "0.01"))).doesNotThrowAnyException();
    }

    @Test
    void distinctAccounts_rejectsSameAccount_acceptsDifferent() {
        DistinctAccountsValidator validator = new DistinctAccountsValidator();
        assertThat(validator.phase()).isEqualTo(ValidationPhase.PRE_LOCK);

        assertThatThrownBy(() -> validator.validate(request(7L, 7L, "10.00")))
                .isInstanceOf(InvalidTransferException.class);
        assertThatCode(() -> validator.validate(request(7L, 8L, "10.00"))).doesNotThrowAnyException();
    }

    @Test
    void sameCurrency_rejectsMismatch_acceptsMatch() {
        SameCurrencyValidator validator = new SameCurrencyValidator();
        assertThat(validator.phase()).isEqualTo(ValidationPhase.POST_LOCK);

        assertThatThrownBy(() -> validator.validate(locked(Currency.TRY, "100.00", Currency.USD, "10.00")))
                .isInstanceOf(InvalidTransferException.class);
        assertThatCode(() -> validator.validate(locked(Currency.TRY, "100.00", Currency.TRY, "10.00")))
                .doesNotThrowAnyException();
    }

    @Test
    void sufficientBalance_rejectsWhenShort_acceptsWhenEnoughOrExact() {
        SufficientBalanceValidator validator = new SufficientBalanceValidator();
        assertThat(validator.phase()).isEqualTo(ValidationPhase.POST_LOCK);

        assertThatThrownBy(() -> validator.validate(locked(Currency.TRY, "10.00", Currency.TRY, "10.01")))
                .isInstanceOf(InsufficientBalanceException.class);
        assertThatCode(() -> validator.validate(locked(Currency.TRY, "10.00", Currency.TRY, "10.00")))
                .doesNotThrowAnyException(); // exact balance is allowed
        assertThatCode(() -> validator.validate(locked(Currency.TRY, "10.00", Currency.TRY, "9.99")))
                .doesNotThrowAnyException();
    }
}

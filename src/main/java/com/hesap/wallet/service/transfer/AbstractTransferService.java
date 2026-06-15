package com.hesap.wallet.service.transfer;

import com.hesap.wallet.dto.request.TransferRequest;
import com.hesap.wallet.dto.response.TransferResponse;
import org.springframework.transaction.annotation.Transactional;

/**
 * Template Method (GoF Behavioral) defining the immutable transfer sequence. The skeleton
 * lives here in exactly one place; subclasses fill in the step hooks but can never reorder
 * the steps. Fixing the order is what makes the engine race-safe: locks are always acquired
 * (step 2) before any balance is read for the funds check or mutated (step 3).
 *
 * <p>The whole sequence runs in a single transaction, so a failure in any step rolls back
 * every balance change and ledger write. {@code transfer} is intentionally non-final so
 * Spring's CGLIB proxy can apply the {@code @Transactional} advice; the Template Method
 * contract is preserved because subclasses override only the protected hooks below.
 */
public abstract class AbstractTransferService {

    /**
     * Immutable order: validate -> acquire pessimistic locks -> update balances -> write ledger.
     */
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        TransferContext context = new TransferContext(
                request.senderAccountId(), request.receiverAccountId(), request.amount());

        validate(context);            // step 1: stateless request validation
        acquireLocks(context);        // step 2: pessimistic locks, ascending id order
        updateBalances(context);      // step 3: validate under lock, then debit/credit
        return writeLedger(context);  // step 4: linked TRANSFER_OUT / TRANSFER_IN entries
    }

    protected abstract void validate(TransferContext context);

    protected abstract void acquireLocks(TransferContext context);

    protected abstract void updateBalances(TransferContext context);

    protected abstract TransferResponse writeLedger(TransferContext context);
}

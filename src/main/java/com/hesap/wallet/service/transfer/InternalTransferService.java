package com.hesap.wallet.service.transfer;

import com.hesap.wallet.dto.response.TransferResponse;
import com.hesap.wallet.entity.Account;
import com.hesap.wallet.entity.Transaction;
import com.hesap.wallet.exception.AccountNotFoundException;
import com.hesap.wallet.factory.TransactionFactory;
import com.hesap.wallet.repository.AccountRepository;
import com.hesap.wallet.repository.TransactionRepository;
import com.hesap.wallet.service.transfer.validation.TransferValidationStrategy;
import com.hesap.wallet.service.transfer.validation.ValidationPhase;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Concrete transfer engine: implements the {@link AbstractTransferService} step hooks for
 * an internal account-to-account transfer. Validation rules are supplied as Strategy beans
 * and partitioned by phase, so new rules can be added without touching this class.
 */
@Service
public class InternalTransferService extends AbstractTransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionFactory transactionFactory;
    private final List<TransferValidationStrategy> preLockValidators;
    private final List<TransferValidationStrategy> postLockValidators;

    public InternalTransferService(AccountRepository accountRepository,
                                   TransactionRepository transactionRepository,
                                   TransactionFactory transactionFactory,
                                   List<TransferValidationStrategy> validators) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionFactory = transactionFactory;
        this.preLockValidators = validators.stream()
                .filter(v -> v.phase() == ValidationPhase.PRE_LOCK)
                .toList();
        this.postLockValidators = validators.stream()
                .filter(v -> v.phase() == ValidationPhase.POST_LOCK)
                .toList();
    }

    @Override
    protected void validate(TransferContext context) {
        preLockValidators.forEach(validator -> validator.validate(context));
    }

    @Override
    protected void acquireLocks(TransferContext context) {
        // Lock in ascending id order so concurrent A->B and B->A transfers can never deadlock.
        boolean senderFirst = context.getSenderAccountId() <= context.getReceiverAccountId();
        Long firstId = senderFirst ? context.getSenderAccountId() : context.getReceiverAccountId();
        Long secondId = senderFirst ? context.getReceiverAccountId() : context.getSenderAccountId();

        Account first = lockAccount(firstId);
        Account second = lockAccount(secondId);

        context.setSender(senderFirst ? first : second);
        context.setReceiver(senderFirst ? second : first);
    }

    @Override
    protected void updateBalances(TransferContext context) {
        // Funds/currency checks read the lock-protected accounts, immediately before the debit.
        postLockValidators.forEach(validator -> validator.validate(context));

        Account sender = context.getSender();
        Account receiver = context.getReceiver();
        sender.setBalance(sender.getBalance().subtract(context.getAmount()));
        receiver.setBalance(receiver.getBalance().add(context.getAmount()));
        accountRepository.save(sender);
        accountRepository.save(receiver);
    }

    @Override
    protected TransferResponse writeLedger(TransferContext context) {
        String reference = UUID.randomUUID().toString();
        context.setTransferReference(reference);

        Transaction out = transactionFactory.transferOut(
                context.getSender(), context.getAmount(), context.getReceiverAccountId(), reference);
        Transaction in = transactionFactory.transferIn(
                context.getReceiver(), context.getAmount(), context.getSenderAccountId(), reference);

        Transaction savedOut = transactionRepository.save(out);
        transactionRepository.save(in);

        return TransferResponse.from(savedOut);
    }

    private Account lockAccount(Long id) {
        return accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}

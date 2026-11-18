package com.minibank;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.minibank.account.AccountDtos.CreateAccountRequest;
import com.minibank.account.AccountService;
import com.minibank.transfer.TransferDtos.TransferRequest;
import com.minibank.transfer.TransferService;
import com.minibank.transfer.TransferStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Fires lots of transfers at the same time. Without row locking, two threads could both read "£100", both pass the
 * balance check and both spend it.
 */
@SpringBootTest
class ConcurrencyTest {

    @Autowired
    AccountService accounts;

    @Autowired
    TransferService transfers;

    @Test
    void twentySimultaneousTransfersCantOverdrawAnAccount() throws Exception {
        long from = accounts.open(new CreateAccountRequest("Alice", new BigDecimal("100.00"))).id();
        long to = accounts.open(new CreateAccountRequest("Bob", new BigDecimal("0.00"))).id();

        List<TransferStatus> results = runAtOnce(20,
                () -> transfers.transfer(new TransferRequest(from, to, new BigDecimal("10.00")), null).transfer().status());

        assertThat(results).filteredOn(s -> s == TransferStatus.COMPLETED).hasSize(10);
        assertThat(results).filteredOn(s -> s == TransferStatus.REJECTED).hasSize(10);
        assertThat(accounts.get(from).balance()).isEqualByComparingTo("0.00");
        assertThat(accounts.get(to).balance()).isEqualByComparingTo("100.00");
    }

    @Test
    void transfersBothWaysAtOnceDontDeadlockAndNoMoneyIsLost() throws Exception {
        long a = accounts.open(new CreateAccountRequest("Alice", new BigDecimal("500.00"))).id();
        long b = accounts.open(new CreateAccountRequest("Bob", new BigDecimal("500.00"))).id();

        List<Callable<TransferStatus>> jobs = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            long from = i % 2 == 0 ? a : b;
            long to = i % 2 == 0 ? b : a;
            jobs.add(() -> transfers.transfer(new TransferRequest(from, to, new BigDecimal("7.00")), null).transfer().status());
        }
        List<TransferStatus> results = runAtOnce(jobs);

        assertThat(results).allMatch(s -> s == TransferStatus.COMPLETED);
        BigDecimal total = accounts.get(a).balance().add(accounts.get(b).balance());
        assertThat(total).isEqualByComparingTo("1000.00");
    }

    private static <T> List<T> runAtOnce(int copies, Callable<T> job) throws Exception {
        List<Callable<T>> jobs = new ArrayList<>();
        for (int i = 0; i < copies; i++) {
            jobs.add(job);
        }
        return runAtOnce(jobs);
    }

    /** Starts every job at the same instant (a latch holds them all at the starting line). */
    private static <T> List<T> runAtOnce(List<Callable<T>> jobs) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(jobs.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> job : jobs) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return job.call();
                }));
            }
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> f : futures) {
                results.add(f.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}

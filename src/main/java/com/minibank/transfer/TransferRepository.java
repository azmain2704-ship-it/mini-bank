package com.minibank.transfer;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    /** Completed transfers out of an account since a point in time: used for the "5 in a minute" fraud rule. */
    @Query("""
            select count(t) from Transfer t
            where t.fromAccount.id = :accountId and t.status = com.minibank.transfer.TransferStatus.COMPLETED
              and t.createdAt > :since""")
    long countCompletedFromSince(@Param("accountId") Long accountId, @Param("since") Instant since);

    @Query("""
            select t from Transfer t
            where t.fromAccount.id = :accountId or t.toAccount.id = :accountId
            order by t.createdAt desc, t.id desc""")
    List<Transfer> findHistoryForAccount(@Param("accountId") Long accountId);

    List<Transfer> findAllByOrderByCreatedAtDescIdDesc();
}

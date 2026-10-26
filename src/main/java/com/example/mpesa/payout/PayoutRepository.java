package com.example.mpesa.payout;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PayoutRepository extends JpaRepository<Payout, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payout p where p.payoutId = :payoutId")
    Optional<Payout> findByIdForUpdate(@Param("payoutId") String payoutId);

    List<Payout> findByStatusAndUpdatedAtBefore(String status, Instant before);
}

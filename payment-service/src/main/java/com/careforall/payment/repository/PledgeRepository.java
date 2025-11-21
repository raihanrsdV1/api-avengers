package com.careforall.payment.repository;

import com.careforall.payment.entity.Pledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PledgeRepository extends JpaRepository<Pledge, Long> {
    
    Optional<Pledge> findByPaymentGatewayId(String paymentGatewayId);
    
    Optional<Pledge> findByIdempotencyKey(String idempotencyKey);
}

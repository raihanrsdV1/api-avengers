package com.careforall.payment.repository;

import com.careforall.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentGatewayId(String paymentGatewayId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByPledgeId(Long pledgeId);
}

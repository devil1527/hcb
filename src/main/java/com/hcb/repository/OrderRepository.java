package com.hcb.repository;

import com.hcb.model.entity.Order;
import com.hcb.model.enums.OrderStatus;
import com.hcb.model.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByIdempotencyToken(String idempotencyToken);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<Order> findByPaymentStatusOrderByCreatedAtDesc(PaymentStatus paymentStatus);

    List<Order> findAllByOrderByCreatedAtDesc();

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT o FROM Order o WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(o.customerName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(o.customerEmail) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(o.customerMobile) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) " +
           "ORDER BY o.createdAt DESC")
    Page<Order> searchOrders(@Param("query") String query,
                             @Param("status") OrderStatus status,
                             @Param("paymentStatus") PaymentStatus paymentStatus,
                             Pageable pageable);

    long countByPaymentStatus(PaymentStatus paymentStatus);

    long countByStatus(OrderStatus status);
}

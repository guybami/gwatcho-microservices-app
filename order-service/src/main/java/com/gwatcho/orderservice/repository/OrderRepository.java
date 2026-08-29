package com.gwatcho.orderservice.repository;

import com.gwatcho.orderservice.entity.Order;
import com.gwatcho.orderservice.entity.OrderStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository
        extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(
            Long customerId
    );

    List<Order> findByStatus(
            OrderStatus status
    );

    @Query("""
    select distinct o
    from Order o
    left join fetch o.items
    where o.id = :id
""")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
}
package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.model.PendingReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingReservationRepository extends JpaRepository<PendingReservation, String> {
}

package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM ProcessedEvent p WHERE p.eventId = :eventId AND p.consumerName = :consumerName")
    boolean existsByEventIdAndConsumerName(@Param("eventId") String eventId, @Param("consumerName") String consumerName);
}
